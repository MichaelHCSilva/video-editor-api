package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.metrics.VideoOverlayServiceMetrics;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoOverlay;
import com.l8group.videoeditor.rabbit.producer.VideoOverlayProducer;
import com.l8group.videoeditor.repositories.VideoOverlayRepository;
import com.l8group.videoeditor.requests.VideoOverlayRequest;
import com.l8group.videoeditor.utils.FileStorageUtils;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.utils.VideoOverlayUtils;
import com.l8group.videoeditor.validation.VideoOverlayValidator;

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
public class VideoOverlayService {

    private final VideoOverlayRepository videoOverlayRepository;
    private final VideoOverlayProducer videoOverlayProducer;
    private final VideoOverlayServiceMetrics videoOverlayServiceMetrics;
    private final VideoStatusManagerService videoStatusManagerService;
    private final VideoFileFinderService videoFileFinderService;
    private final VideoOverlayValidator videoOverlayValidator;

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String processOverlay(VideoOverlayRequest request, String previousFilePath) {
        videoOverlayServiceMetrics.incrementOverlayRequests();
        logRequest(request, previousFilePath);

        validateRequest(request);

        UUID videoId = UUID.fromString(request.getVideoId());
        VideoFile videoFile = videoFileFinderService.findById(videoId);

        String inputFilePath = FileStorageUtils.resolveInputFilePath(previousFilePath, videoFile.getVideoFilePath());
        ensureInputFileExists(inputFilePath);

        String outputFilePath = prepareOutputFile(videoFile.getVideoFileName());
        var timerSample = videoOverlayServiceMetrics.startOverlayProcessingTimer();

        try {
            applyOverlayWithHandling(inputFilePath, outputFilePath, request);
        } finally {
            videoOverlayServiceMetrics.recordOverlayProcessingDuration(timerSample);
        }

        long fileSize = new File(outputFilePath).length();
        videoOverlayServiceMetrics.setOverlayFileSize(fileSize);

        VideoOverlay videoOverlay = saveOverlayMetadata(videoFile, request);
        updateStatusToCompleted(videoOverlay.getId());

        return outputFilePath;
    }

    private void logRequest(VideoOverlayRequest request, String previousFilePath) {
        log.info("Overlay Request → videoId={}, watermark='{}', position={}, fontSize={}, previousFilePath={}",
                request.getVideoId(), request.getWatermark(), request.getPosition(),
                request.getFontSize(), previousFilePath);
    }

    private void validateRequest(VideoOverlayRequest request) {
        try {
            videoOverlayValidator.validate(request);
        } catch (IllegalArgumentException e) {
            videoOverlayServiceMetrics.incrementOverlayFailure();
            log.warn("Validação falhou para o overlay: {}", e.getMessage());
            throw e;
        }
    }

    private void ensureInputFileExists(String inputFilePath) {
        try {
            FileStorageUtils.validateInputFileExists(inputFilePath,
                    videoOverlayServiceMetrics::incrementOverlayFailure);
        } catch (RuntimeException e) {
            log.error("Erro ao validar arquivo de entrada: {}", inputFilePath, e);
            throw new VideoProcessingException("Erro ao validar arquivo de entrada.", e);
        }
    }

    private String prepareOutputFile(String originalFileName) {
        FileStorageUtils.createDirectoryIfNotExists(TEMP_DIR);
        String outputFile = VideoFileNameGenerator.generateFileNameWithSuffix(originalFileName, "overlay");
        String fullPath = FileStorageUtils.buildFilePath(TEMP_DIR, outputFile);
        log.info("Output overlay path: {}", fullPath);
        return fullPath;
    }

    private void applyOverlayWithHandling(String inputFile, String outputFile, VideoOverlayRequest request) {
        try {
            boolean success = VideoOverlayUtils.applyTextOverlayWithFFmpeg(
                    inputFile, outputFile, request.getWatermark(), request.getPosition(), request.getFontSize(), null);
            if (!success) {
                handleFailure("FFmpeg retornou falha no overlay.");
            } else {
                videoOverlayServiceMetrics.incrementOverlaySuccess();
            }
        } catch (Exception e) {
            handleFailure("Erro inesperado ao aplicar overlay com FFmpeg.", e);
        }
    }

    private void handleFailure(String message) {
        videoOverlayServiceMetrics.incrementOverlayFailure();
        videoOverlayServiceMetrics.decrementProcessingQueueSize();
        throw new VideoProcessingException(message);
    }

    private void handleFailure(String message, Exception e) {
        videoOverlayServiceMetrics.incrementOverlayFailure();
        videoOverlayServiceMetrics.decrementProcessingQueueSize();
        log.error(message, e);
        throw new VideoProcessingException(message, e);
    }

    private VideoOverlay saveOverlayMetadata(VideoFile videoFile, VideoOverlayRequest request) {
        VideoOverlay overlay = new VideoOverlay();
        overlay.setVideoFile(videoFile);
        overlay.setCreatedTimes(ZonedDateTime.now());
        overlay.setUpdatedTimes(ZonedDateTime.now());
        overlay.setStatus(VideoStatusEnum.PROCESSING);
        overlay.setOverlayText(request.getWatermark());
        overlay.setOverlayPosition(request.getPosition());
        overlay.setOverlayFontSize(request.getFontSize());

        VideoOverlay saved = videoOverlayRepository.save(overlay);
        videoOverlayProducer.sendVideoOverlayMessage(saved.getId().toString());
        videoOverlayServiceMetrics.decrementProcessingQueueSize();

        return saved;
    }

    private void updateStatusToCompleted(UUID overlayId) {
        videoStatusManagerService.updateEntityStatus(videoOverlayRepository, overlayId,
                VideoStatusEnum.COMPLETED, "VideoOverlayService - Overlay finalizado com sucesso");
    }

    public void deleteTemporaryFiles(String filePath) {
        FileStorageUtils.deleteFileIfExists(new File(filePath));
    }
}
