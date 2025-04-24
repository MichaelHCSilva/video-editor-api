package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.metrics.VideoConversionServiceMetrics;
import com.l8group.videoeditor.models.VideoConversion;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.rabbit.producer.VideoConversionProducer;
import com.l8group.videoeditor.repositories.VideoConversionRepository;
import com.l8group.videoeditor.requests.VideoConversionRequest;
import com.l8group.videoeditor.utils.FileStorageUtils;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.utils.VideoProcessorUtils;
import com.l8group.videoeditor.validation.VideoConversionValidator;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoConversionService {

    private final VideoFileFinderService videoFileFinderService;
    private final VideoConversionRepository videoConversionRepository;
    private final VideoConversionProducer videoConversionProducer;
    private final VideoConversionServiceMetrics videoConversionServiceMetrics;
    private final VideoStatusManagerService videoStatusManagerService;
    private final VideoConversionValidator videoConversionValidator;

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String convertVideo(VideoConversionRequest request, String previousFilePath) {
        videoConversionServiceMetrics.incrementConversionRequests();
        videoConversionServiceMetrics.incrementProcessingQueueSize();
        Timer.Sample timer = videoConversionServiceMetrics.startConversionTimer();

        UUID videoId = UUID.fromString(request.getVideoId());
        String outputFormat = request.getOutputFormat();

        videoConversionValidator.validateVideoId(request.getVideoId());
        videoConversionValidator.validateOutputFormat(request.getOutputFormat());

        log.info("Iniciando conversão do vídeo com ID: {}, para o formato: {}, arquivo de origem: {}",
                videoId, outputFormat, previousFilePath);

        VideoFile videoFile = videoFileFinderService.findById(videoId);
        String inputFilePath = FileStorageUtils.resolveInputFilePath(previousFilePath, videoFile.getVideoFilePath());

        try {
            FileStorageUtils.validateInputFileExists(inputFilePath, () -> {
                videoConversionServiceMetrics.incrementConversionFailure();
                videoConversionServiceMetrics.decrementProcessingQueueSize();
                log.error("Arquivo de entrada não encontrado: {}", inputFilePath);
            });
        } catch (RuntimeException e) {
            log.error("Erro ao validar arquivo de entrada: {}", inputFilePath, e);
            throw new VideoProcessingException("Erro ao processar o vídeo: arquivo de origem não encontrado.", e);
        }

        FileStorageUtils.createDirectoryIfNotExists(TEMP_DIR);
        String outputFileName = VideoFileNameGenerator.generateFileNameWithSuffix(videoFile.getVideoFileName(),
                "convert");
        String outputFilePath = FileStorageUtils.buildFilePath(TEMP_DIR, outputFileName);

        log.info("Processando conversão: {} → {} (Formato: {})", inputFilePath, outputFilePath, outputFormat);

        boolean success = VideoProcessorUtils.convertVideo(inputFilePath, outputFilePath, outputFormat);
        if (!success) {
            handleConversionFailure(outputFormat);
        }

        postConversionSuccess(timer, outputFilePath);

        VideoConversion videoConversion = createAndSaveVideoConversion(videoFile, outputFormat);
        videoStatusManagerService.updateEntityStatus(videoConversionRepository, videoConversion.getId(),
                VideoStatusEnum.COMPLETED, "Conversão concluída com sucesso.");

        log.info("Conversão do vídeo {} para o formato {} concluída. Arquivo de saída: {}",
                videoId, outputFormat, outputFilePath);

        return outputFilePath;
    }

    private void handleConversionFailure(String outputFormat) {
        log.error("Falha ao converter o vídeo para o formato {}", outputFormat);
        videoConversionServiceMetrics.incrementConversionFailure();
        videoConversionServiceMetrics.decrementProcessingQueueSize();
        throw new RuntimeException("Falha ao converter o vídeo para o formato " + outputFormat);
    }

    private void postConversionSuccess(Timer.Sample timer, String outputFilePath) {
        long fileSize = new File(outputFilePath).length();
        videoConversionServiceMetrics.setConvertedFileSize(fileSize);
        videoConversionServiceMetrics.recordConversionDuration(timer);
        videoConversionServiceMetrics.incrementConversionSuccess();
        videoConversionServiceMetrics.decrementProcessingQueueSize();
    }

    private VideoConversion createAndSaveVideoConversion(VideoFile videoFile, String outputFormat) {
        VideoConversion videoConversion = new VideoConversion();
        videoConversion.setVideoFile(videoFile);
        videoConversion.setVideoTargetFormat(outputFormat);
        videoConversion.setVideoFileFormat(videoFile.getVideoFileFormat());
        videoConversion.setCreatedTimes(ZonedDateTime.now());
        videoConversion.setUpdatedTimes(ZonedDateTime.now());
        videoConversion.setStatus(VideoStatusEnum.PROCESSING);

        videoConversion = videoConversionRepository.save(videoConversion);
        videoConversionProducer.sendVideoConversionMessage(videoConversion.getId().toString());
        return videoConversion;
    }

    public void deleteTemporaryFiles(String filePath) {
        FileStorageUtils.deleteFileIfExists(new File(filePath));
    }
}