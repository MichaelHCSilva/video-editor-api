package com.l8group.videoeditor.rabbit.consumer;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;

@Service
public class VideoProcessingConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.VIDEO_PROCESSING_QUEUE)
    public void processVideo(String videoIdStr) {
        try {
            UUID videoId = UUID.fromString(videoIdStr);
            logger.info("Mensagem recebida para processamento do vídeo: {}", videoId);

            // 🔹 Agora o status só será alterado pelo S3Service!

        } catch (IllegalArgumentException e) {
            logger.error("Erro ao converter UUID. String '{}' não é um UUID válido. Detalhes: {}", videoIdStr,
                    e.getMessage());
        } catch (Exception e) {
            logger.error("Erro ao processar VideoFile com ID '{}'. Detalhes: {}", videoIdStr, e.getMessage());
        }
    }
}
