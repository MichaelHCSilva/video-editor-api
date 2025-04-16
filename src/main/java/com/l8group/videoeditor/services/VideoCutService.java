package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.InvalidCutTimeException;
import com.l8group.videoeditor.exceptions.InvalidMediaPropertiesException;
import com.l8group.videoeditor.exceptions.VideoMetadataException;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.exceptions.VideoDurationParseException; // Nova exceção
import com.l8group.videoeditor.metrics.VideoCutServiceMetrics;
import com.l8group.videoeditor.models.VideoCut;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.rabbit.producer.VideoCutProducer;
import com.l8group.videoeditor.repositories.VideoCutRepository;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.utils.FileStorageUtils;
import com.l8group.videoeditor.utils.VideoDurationUtils;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.utils.VideoProcessorUtils;
import com.l8group.videoeditor.validation.AudioValidator;
import com.l8group.videoeditor.validation.VideoCutValidator;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCutService {

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    private final VideoCutRepository videoCutRepository;
    private final VideoCutProducer videoCutProducer;
    private final VideoCutServiceMetrics videoCutServiceMetrics;
    private final VideoStatusManagerService videoStatusManagerService;
    private final VideoFileFinderService videoFileFinderService;
    private final FilePreparationService filePreparationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String cutVideo(VideoCutRequest request, String previousFilePath) {
        log.info("Iniciando corte do vídeo. videoId={}, startTime={}, endTime={}, previousFilePath={}",
                 request.getVideoId(), request.getStartTime(), request.getEndTime(), previousFilePath);

        videoCutServiceMetrics.incrementCutRequests();

        UUID videoId;
        try {
            videoId = UUID.fromString(request.getVideoId());
        } catch (IllegalArgumentException e) {
            log.error("Formato de ID de vídeo inválido: {}, esperado formato UUID.", request.getVideoId());
            throw new VideoProcessingException("Formato de ID de vídeo inválido. Esperado formato UUID.");
        }

        VideoFile videoFile = videoFileFinderService.findById(videoId);
        if (videoFile == null) {
            log.error("VideoFile não encontrado para o ID: {}", videoId);
            throw new VideoProcessingException("Vídeo não encontrado.");
        }
        String inputFilePath = filePreparationService.prepareVideoFileAndInputPath(videoFile);

        String durationString;
        try {
            durationString = VideoDurationUtils.getVideoDurationAsString(inputFilePath);
            log.info("Duração do vídeo: {}", durationString);
        } catch (IOException e) {
            log.error("Erro ao obter duração do vídeo: {}", e.getMessage(), e);
            throw new VideoMetadataException("Erro ao obter duração do vídeo.");
        }

        int startTime;
        int endTime;
        try {
            startTime = VideoDurationUtils.convertTimeToSeconds(request.getStartTime());
            endTime = VideoDurationUtils.convertTimeToSeconds(request.getEndTime());
        } catch (IllegalArgumentException e) {
            log.error("Erro ao converter tempo de corte: startTime={}, endTime={}, erro={}",
                      request.getStartTime(), request.getEndTime(), e.getMessage());
            throw new InvalidCutTimeException(String.format(
                    "Formato de tempo de corte inválido para startTime='%s' ou endTime='%s'. Esperado HH:mm:ss.",
                    request.getStartTime(), request.getEndTime()));
        } catch (VideoDurationParseException e) {
            log.error("Erro ao analisar o tempo de corte: {}", e.getMessage());
            throw e; // Propaga a exceção específica
        }

        try {
            VideoCutValidator.validateCutTimes(startTime, endTime, videoFile);
        } catch (InvalidCutTimeException e) {
            log.error("Erro no tempo de corte: {}", e.getMessage());
            throw e;
        }

        try {
            AudioValidator.validateAudioProperties(inputFilePath, startTime, endTime);
        } catch (InvalidMediaPropertiesException e) {
            log.error("Problema nas propriedades de áudio do vídeo: {}", e.getMessage());
            throw e; // Propaga a exceção para interromper o processo
        }

        String outputFilePath = prepareOutputFilePath(videoFile);
        videoCutServiceMetrics.incrementProcessingQueueSize();
        Timer.Sample timerSample = videoCutServiceMetrics.startCutTimer();

        boolean cutSuccess = performCutOperation(inputFilePath, outputFilePath, request);
        if (!cutSuccess) {
            videoCutServiceMetrics.incrementCutFailure();
            videoCutServiceMetrics.decrementProcessingQueueSize();
            log.error("Falha ao processar o corte do vídeo. Input: {}, Output: {}, Request: {}",
                      inputFilePath, outputFilePath, request);
            throw new VideoProcessingException("Falha ao processar o corte do vídeo.");
        }

        videoCutServiceMetrics.recordCutDuration(timerSample);
        updateOutputFileMetrics(outputFilePath);

        // Mecanismo de compensação: Excluir arquivo de saída em caso de falha após a criação
        try {
            int cutDuration = endTime - startTime;
            finalizeCutOperation(videoFile, cutDuration, outputFilePath);
        } catch (Exception e) {
            log.error("Erro ao finalizar operação de corte, tentando compensar (excluir arquivo): {}", e.getMessage());
            deleteTemporaryFiles(outputFilePath);
            throw new VideoProcessingException("Erro ao finalizar o corte do vídeo após a criação do arquivo.", e);
        }

        return outputFilePath;
    }

    private String prepareOutputFilePath(VideoFile videoFile) {
        FileStorageUtils.createDirectoryIfNotExists(TEMP_DIR);
        String outputFileName = VideoFileNameGenerator.generateFileNameWithSuffix(
                videoFile.getVideoFileName(), "cut");
        String outputFilePath = Paths.get(TEMP_DIR, outputFileName).toString();
        log.info("Arquivo de saída definido como: {}", outputFilePath);
        return outputFilePath;
    }

    private boolean performCutOperation(String inputFilePath, String outputFilePath, VideoCutRequest request) {
        log.info("Iniciando corte do vídeo: {} → {}", inputFilePath, outputFilePath);
        boolean success = VideoProcessorUtils.cutVideo(inputFilePath, outputFilePath,
                request.getStartTime(), request.getEndTime());

        if (!success) {
            log.error("Falha ao processar o corte do vídeo.");
            // Não precisa excluir o arquivo aqui, pois a falha impede a criação completa
            return false;
        }

        log.info("Corte concluído com sucesso.");
        videoCutServiceMetrics.incrementCutSuccess();
        return true;
    }

    private void updateOutputFileMetrics(String outputFilePath) {
        File outputFile = new File(outputFilePath);
        if (outputFile.exists()) {
            videoCutServiceMetrics.setCutFileSize(outputFile.length());
        }
    }

    private String finalizeCutOperation(VideoFile videoFile, int cutDurationSeconds, String outputFilePath) {
        String durationFormatted = VideoDurationUtils.formatSecondsToTime(cutDurationSeconds);
        log.info("Duração do vídeo cortado: {}", durationFormatted);

        VideoCut videoCut = saveVideoCut(videoFile, durationFormatted);
        videoCutProducer.sendVideoCutId(videoCut.getId());
        videoStatusManagerService.updateEntityStatus(
                videoCutRepository,
                videoCut.getId(),
                VideoStatusEnum.COMPLETED,
                "CutService - Conclusão");

        return outputFilePath;
    }

    private VideoCut saveVideoCut(VideoFile videoFile, String durationFormatted) {
        VideoCut videoCut = new VideoCut();
        videoCut.setVideoFile(videoFile);
        videoCut.setVideoCutDuration(durationFormatted);
        videoCut.setCreatedTimes(ZonedDateTime.now());
        videoCut.setUpdatedTimes(ZonedDateTime.now());
        videoCut.setStatus(VideoStatusEnum.PROCESSING);

        videoCut = videoCutRepository.save(videoCut);
        log.info("Registro de corte salvo no banco de dados. ID={}, Tempo de registro: {}", videoCut.getId(),
                 LocalDateTime.now());
        return videoCut;
    }

    public void deleteTemporaryFiles(String filePath) {
        File file = new File(filePath);
        FileStorageUtils.deleteFileIfExists(file);
    }
}