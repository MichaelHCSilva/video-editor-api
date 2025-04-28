package com.l8group.videoeditor.tasks;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.services.VideoS3Service;
import com.l8group.videoeditor.services.VideoStatusManagerService;

@Component
public class VideoRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VideoRetryScheduler.class);

    @Value("${video.retry.max-attempts}")
    private int maxRetries;

    private final VideoFileRepository videoFileRepository;
    private final VideoS3Service s3Service;
    private final VideoStatusManagerService videoStatusManagerService;

    public VideoRetryScheduler(VideoFileRepository videoFileRepository, VideoS3Service s3Service,
                               VideoStatusManagerService videoStatusManagerService) {
        this.videoFileRepository = videoFileRepository;
        this.s3Service = s3Service;
        this.videoStatusManagerService = videoStatusManagerService;
    }

    @Scheduled(fixedRateString = "${video.retry.interval-ms}")
    @Transactional
    public void retryFailedUploads() {
        List<VideoFile> failedVideos = videoFileRepository.findByStatus(VideoStatusEnum.ERROR);

        if (failedVideos.isEmpty()) {
            logger.info("Nenhum vídeo com status ERROR encontrado para reprocessamento.");
            return;
        }

        logger.info("Iniciando reprocessamento de {} vídeos com falha.", failedVideos.size());

        for (VideoFile video : failedVideos) {
            try {
                int currentRetryCount = video.getRetryCount(); // Obter o retryCount da entidade

                if (currentRetryCount >= maxRetries) {
                    videoStatusManagerService.updateEntityStatus(videoFileRepository, video.getId(), VideoStatusEnum.FAILED_PERMANENTLY, "RetryScheduler");
                    logger.warn("Vídeo {} atingiu o limite de {} tentativas e foi marcado como FAILED_PERMANENTLY.", video.getId(), maxRetries);
                    continue;
                }

                File file = new File(video.getVideoFilePath());
                if (!file.exists() || !file.isFile()) {
                    logger.warn("Arquivo do vídeo {} não encontrado em '{}'. Pulando reprocessamento.", video.getId(), video.getVideoFilePath());
                    continue;
                }

                String s3Url;
                if (video.getVideoFilePath().contains("raw-videos")) {
                    s3Url = s3Service.uploadRawFile(file, video.getVideoFileName(), video.getId());
                } else if (video.getVideoFilePath().contains("processed-videos")) {
                    s3Url = s3Service.uploadProcessedFile(file, video.getVideoFileName(), video.getId());
                } else {
                    logger.error("Caminho do arquivo inválido para vídeo {}: {}", video.getId(), video.getVideoFilePath());
                    continue;
                }

                video.setVideoFilePath(s3Url);
                video.setRetryCount(0); // Resetar o retryCount aqui
                videoFileRepository.save(video);

                videoStatusManagerService.updateEntityStatus(videoFileRepository, video.getId(), VideoStatusEnum.COMPLETED, "RetryScheduler");

                logger.info("Vídeo {} reenviado com sucesso para S3.", video.getId());

            } catch (IOException e) {
                videoStatusManagerService.updateEntityStatus(videoFileRepository, video.getId(), VideoStatusEnum.ERROR, "RetryScheduler");
                logger.error("Erro ao reenviar vídeo {}: {}. Tentativa {}/{}", video.getId(), e.getMessage(), video.getRetryCount(), maxRetries);
            } catch (Exception e) {
                videoStatusManagerService.updateEntityStatus(videoFileRepository, video.getId(), VideoStatusEnum.ERROR, "RetryScheduler");
                logger.error("Erro inesperado ao reprocessar vídeo {}: {}", video.getId(), e.getMessage());
            }
        }

        logger.info("Reprocessamento finalizado.");
    }
}