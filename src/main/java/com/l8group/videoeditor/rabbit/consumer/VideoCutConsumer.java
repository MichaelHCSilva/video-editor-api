package com.l8group.videoeditor.rabbit.consumer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Service
public class VideoCutConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoCutConsumer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public VideoCutConsumer() {}

    @RabbitListener(queues = RabbitMQConfig.VIDEO_CUT_QUEUE)
    public void processVideoCut(@Payload String videoCutIdStr, Message<?> message) {
        executeWithRetry(() -> {
            try {
                UUID videoCutId = UUID.fromString(videoCutIdStr);
                logger.info("Corte de vídeo {} processado com sucesso. Status atualizado às: {}", videoCutId, LocalDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoCutIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_CUT_DLQ, videoCutIdStr);
                throw new RuntimeException("Erro no processamento do corte de vídeo: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Erro ao processar corte de vídeo com ID '{}'. Detalhes: {}", videoCutIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_CUT_DLQ, videoCutIdStr);
                throw new RuntimeException("Erro no processamento do corte de vídeo: " + e.getMessage(), e);
            }
        });
    }
}
