package com.l8group.videoeditor.rabbit.consumer;

import com.l8group.videoeditor.config.RabbitMQConfig;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.services.VideoS3Service;
import com.l8group.videoeditor.services.VideoStatusManagerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoProcessingConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingConsumer.class);

    private final VideoFileRepository videoFileRepository;
    private final VideoS3Service s3Service;
    private final VideoStatusManagerService videoStatusManagerService;

    @RabbitListener(queues = RabbitMQConfig.VIDEO_PROCESSING_QUEUE)
    public void processVideo(@Payload String videoIdStr, Message<?> message) {
        executeWithRetry(() -> {
            UUID videoId = UUID.fromString(videoIdStr);
            logger.info("Mensagem recebida para processamento do vídeo: {}", videoId);

            VideoFile videoFile = videoFileRepository.findById(videoId)
                    .orElseThrow(() -> {
                        logger.warn("Vídeo ainda não foi persistido no banco. Retentando... ID: {}", videoId);
                        return new IllegalStateException("Vídeo ainda não encontrado no banco");
                    });

            File file = new File(videoFile.getVideoFilePath());
            if (!file.exists()) {
                throw new IllegalStateException("Arquivo local não encontrado: " + videoFile.getVideoFilePath());
            }

            try {
                String s3Url = s3Service.uploadRawFile(file, videoFile.getVideoFileName(), videoId);
                logger.info("Upload para o S3 finalizado com sucesso: {}", s3Url);
            } catch (IOException ioException) {
                throw new RuntimeException("Erro de IO ao fazer upload para o S3", ioException);
            }

            videoStatusManagerService.updateEntityStatus(
                    videoFileRepository, videoId, VideoStatusEnum.COMPLETED, "RabbitConsumerUpload");
        });
    }
}
