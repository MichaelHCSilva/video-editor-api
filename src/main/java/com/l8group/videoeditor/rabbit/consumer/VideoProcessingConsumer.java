package com.l8group.videoeditor.rabbit.consumer;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.services.S3Service;
import com.l8group.videoeditor.services.VideoStatusManagerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoProcessingConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingConsumer.class);

    private final VideoFileRepository videoFileRepository;
    private final S3Service s3Service;
    private final VideoStatusManagerService videoStatusManagerService;

    @RabbitListener(queues = RabbitMQConfig.VIDEO_PROCESSING_QUEUE)
    public void processVideo(String videoIdStr) {
        executeWithRetry(() -> {
            try {
                UUID videoId = UUID.fromString(videoIdStr);
                logger.info("Mensagem recebida para processamento do vídeo: {}", videoId);

                VideoFile videoFile = videoFileRepository.findById(videoId)
                        .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado: " + videoId));

                File file = new File(videoFile.getVideoFilePath());
                if (!file.exists()) {
                    throw new IllegalStateException("Arquivo local não encontrado: " + videoFile.getVideoFilePath());
                }

                String s3Url;
                try {
                    s3Url = s3Service.uploadRawFile(file, videoFile.getVideoFileName(), videoId);
                } catch (IOException ioException) {
                    logger.error("Erro de IO ao tentar fazer upload para o S3 do vídeo ID '{}': {}", videoId, ioException.getMessage());
                    throw new RuntimeException("Erro ao fazer upload para o S3", ioException);
                }

                videoFile.setVideoFilePath(s3Url);
                videoFileRepository.saveAndFlush(videoFile);

                videoStatusManagerService.updateEntityStatus(
                        videoFileRepository, videoId, VideoStatusEnum.COMPLETED, "RabbitConsumerUpload");

                logger.info("Upload para o S3 finalizado com sucesso: {}", s3Url);

            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID. String '{}' não é um UUID válido. Detalhes: {}", videoIdStr, e.getMessage());
                throw e;
            } catch (Exception e) {
                logger.error("Erro ao processar VideoFile com ID '{}'. Detalhes: {}", videoIdStr, e.getMessage());
                throw e;
            }
        });
    }
}
