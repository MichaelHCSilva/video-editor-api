package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.InvalidCutTimeException;
import com.l8group.videoeditor.exceptions.InvalidMediaPropertiesException;
import com.l8group.videoeditor.exceptions.VideoDurationParseException;
import com.l8group.videoeditor.exceptions.VideoMetadataException;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

        UUID videoId = parseVideoId(request.getVideoId());
        VideoFile videoFile = findVideoFile(videoId);
        String inputFilePath = filePreparationService.prepareVideoFileAndInputPath(videoFile);
        getVideoDuration(inputFilePath);

        int startTime = parseAndValidateCutTime(request.getStartTime(), "startTime");
        int endTime = parseAndValidateCutTime(request.getEndTime(), "endTime");

        validateCutTimes(startTime, endTime, videoFile);
        validateAudioProperties(inputFilePath, startTime, endTime);

        String outputFilePath = prepareOutputFilePath(videoFile);
        videoCutServiceMetrics.incrementProcessingQueueSize();
        Timer.Sample timerSample = videoCutServiceMetrics.startCutTimer();

        boolean cutSuccess = performCutOperation(inputFilePath, outputFilePath, request);
        if (!cutSuccess) {
            handleCutFailure(inputFilePath, outputFilePath, request);
            throw new VideoProcessingException("Falha ao processar o corte do vídeo.");
        }

        videoCutServiceMetrics.recordCutDuration(timerSample);
        updateOutputFileMetrics(outputFilePath);

        return finalizeCutOperation(videoFile, startTime, endTime, outputFilePath);
    }

    private UUID parseVideoId(String videoIdStr) {
        try {
            return UUID.fromString(videoIdStr);
        } catch (IllegalArgumentException e) {
            log.error("Formato de ID de vídeo inválido: {}, esperado formato UUID.", videoIdStr);
            throw new VideoProcessingException("Formato de ID de vídeo inválido. Esperado formato UUID.");
        }
    }

    private VideoFile findVideoFile(UUID videoId) {
        VideoFile videoFile = videoFileFinderService.findById(videoId);
        if (videoFile == null) {
            log.error("VideoFile não encontrado para o ID: {}", videoId);
            throw new VideoProcessingException("Vídeo não encontrado.");
        }
        return videoFile;
    }

    private String getVideoDuration(String inputFilePath) {
        try {
            String durationString = VideoDurationUtils.getVideoDurationAsString(inputFilePath);
            log.info("Duração do vídeo: {}", durationString);
            return durationString;
        } catch (IOException e) {
            log.error("Erro ao obter duração do vídeo: {}", e.getMessage(), e);
            throw new VideoMetadataException("Erro ao obter duração do vídeo.");
        }
    }

    private int parseAndValidateCutTime(String timeStr, String timePoint) {
        try {
            return VideoDurationUtils.convertTimeToSeconds(timeStr);
        } catch (IllegalArgumentException e) {
            log.error("Erro ao converter tempo de corte: {}={}, erro={}", timePoint, timeStr, e.getMessage());
            throw new InvalidCutTimeException(String.format(
                "Formato de tempo de corte inválido para %s='%s'. Esperado HH:mm:ss.", timePoint, timeStr));
        } catch (VideoDurationParseException e) {
            log.error("Erro ao analisar o tempo de corte: {}", e.getMessage());
            throw e; // Propaga a exceção específica
        }
    }

    private void validateCutTimes(int startTime, int endTime, VideoFile videoFile) {
        try {
            VideoCutValidator.validateCutTimes(startTime, endTime, videoFile);
        } catch (InvalidCutTimeException e) {
            log.error("Erro no tempo de corte: {}", e.getMessage());
            throw e;
        }
    }

    private void validateAudioProperties(String inputFilePath, int startTime, int endTime) {
        try {
            AudioValidator.validateAudioProperties(inputFilePath, startTime, endTime);
        } catch (InvalidMediaPropertiesException e) {
            log.error("Problema nas propriedades de áudio do vídeo: {}", e.getMessage());
            throw e; 
        }
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
        log.info("Corte {} com sucesso.", success ? "concluído" : "falhou");
        if (success) {
            videoCutServiceMetrics.incrementCutSuccess();
        }
        return success;
    }

    private void handleCutFailure(String inputFilePath, String outputFilePath, VideoCutRequest request) {
        videoCutServiceMetrics.incrementCutFailure();
        videoCutServiceMetrics.decrementProcessingQueueSize();
        log.error("Falha ao processar o corte do vídeo. Input: {}, Output: {}, Request: {}",
            inputFilePath, outputFilePath, request);
    }

    private void updateOutputFileMetrics(String outputFilePath) {
        File outputFile = new File(outputFilePath);
        if (outputFile.exists()) {
            videoCutServiceMetrics.setCutFileSize(outputFile.length());
        }
    }

    private String finalizeCutOperation(VideoFile videoFile, int startTime, int endTime, String outputFilePath) {
        int cutDurationSeconds = endTime - startTime;
        String durationFormatted = VideoDurationUtils.formatSecondsToTime(cutDurationSeconds);
        log.info("Duração do vídeo cortado: {}", durationFormatted);

        VideoCut videoCut = saveVideoCut(videoFile, durationFormatted);
        videoCutProducer.sendVideoCutId(videoCut.getId());
        updateVideoCutStatusCompleted(videoCut.getId());

        return outputFilePath;
    }

    private void updateVideoCutStatusCompleted(UUID videoCutId) {
        videoStatusManagerService.updateEntityStatus(
            videoCutRepository,
            videoCutId,
            VideoStatusEnum.COMPLETED,
            "CutService - Conclusão");
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