package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.metrics.VideoOverlayMetrics;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoOverlay;
import com.l8group.videoeditor.rabbit.producer.VideoOverlayProducer;
import com.l8group.videoeditor.repositories.VideoOverlayRepository;
import com.l8group.videoeditor.requests.VideoOverlayRequest;
import com.l8group.videoeditor.utils.VideoFileStorageUtils;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.utils.VideoOverlayUtils;
import com.l8group.videoeditor.validation.VideoOverlayValidation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoOverlayService {

    private final VideoOverlayRepository videoOverlayRepository;
    private final VideoOverlayProducer videoOverlayProducer;
    private final VideoOverlayMetrics metrics;
    private final VideoFileFinderService videoFileFinderService;
    private final VideoOverlayValidation videoOverlayValidator;
    private final VideoStatusService videoStatusManagerService;

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    @Transactional
    public String processOverlay(VideoOverlayRequest request, String previousFilePath) {
        log.info("Iniciando processo de overlay para vídeo ID: {}", request.getVideoId());
        metrics.incrementOverlayRequests();
        metrics.incrementProcessingQueueSize();

        var sample = metrics.startOverlayProcessingTimer();

        String videoId = request.getVideoId();
        videoOverlayValidator.validate(request);

        VideoFile videoFile = videoFileFinderService.findById(videoId);
        String inputFilePath = previousFilePath != null ? previousFilePath : videoFile.getVideoFilePath();
        validateInputFile(inputFilePath);

        String outputFilePath = prepareOutputFile(videoFile.getVideoFileName());

        VideoOverlay overlayEntity = saveOverlayEntity(videoFile, request);

        try {
            log.info("Aplicando overlay com texto '{}', posição '{}' e fonte {}",
                    request.getWatermark(), request.getPosition(), request.getFontSize());

            boolean success = VideoOverlayUtils.applyTextOverlayWithFFmpeg(
                    inputFilePath, outputFilePath,
                    request.getWatermark(), request.getPosition(), request.getFontSize(), null
            );

            if (!success) {
                log.error("Falha na aplicação do overlay via FFmpeg.");
                metrics.incrementOverlayFailure();
                throw new VideoProcessingException("Erro ao aplicar overlay.");
            }

            metrics.incrementOverlaySuccess();

            long fileSize = new File(outputFilePath).length();
            metrics.setOverlayFileSize(fileSize);

            videoStatusManagerService.updateEntityStatus(
                    videoOverlayRepository, overlayEntity.getId(), VideoStatusEnum.COMPLETED, "Overlay concluído com sucesso");

            log.info("Overlay aplicado com sucesso. Tamanho do arquivo de saída: {} bytes", fileSize);

        } catch (Exception e) {
            log.error("Erro durante o processamento de overlay: {}", e.getMessage(), e);
            metrics.incrementOverlayFailure();
            videoStatusManagerService.updateEntityStatus(
                    videoOverlayRepository, overlayEntity.getId(), VideoStatusEnum.ERROR, "Erro inesperado no overlay");
            throw new VideoProcessingException("Erro inesperado ao aplicar overlay.", e);
        } finally {
            metrics.recordOverlayProcessingDuration(sample);
            metrics.decrementProcessingQueueSize();
        }

        videoOverlayProducer.sendVideoOverlayMessage(videoId);
        log.info("Processo de overlay finalizado com sucesso.");
        return outputFilePath;
    }

    private void validateInputFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new VideoProcessingException("Arquivo de entrada inexistente.");
        }
    }

    private String prepareOutputFile(String originalFileName) {
        VideoFileStorageUtils.createDirectoryIfNotExists(TEMP_DIR);
        return VideoFileStorageUtils.buildFilePath(
                TEMP_DIR, VideoFileNameGenerator.generateFileNameWithSuffix(originalFileName, "overlay"));
    }

    private VideoOverlay saveOverlayEntity(VideoFile videoFile, VideoOverlayRequest request) {
        VideoOverlay overlay = new VideoOverlay();
        overlay.setVideoFile(videoFile);
        overlay.setOverlayText(request.getWatermark());
        overlay.setOverlayPosition(request.getPosition());
        overlay.setOverlayFontSize(request.getFontSize());
        overlay.setStatus(VideoStatusEnum.PROCESSING);
        overlay.setCreatedTimes(ZonedDateTime.now());
        overlay.setUpdatedTimes(ZonedDateTime.now());
        return videoOverlayRepository.save(overlay);
    }

    public void deleteTemporaryFiles(String filePath) {
        VideoFileStorageUtils.deleteFileIfExists(new File(filePath));
    }
}
