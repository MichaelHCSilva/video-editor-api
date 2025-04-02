package com.l8group.videoeditor.rabbit.consumer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;

@Service
public class VideoCutConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoCutConsumer.class);

    public VideoCutConsumer() {
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_CUT_QUEUE)
    public void processVideoCut(String videoCutIdStr) {
        executeWithRetry(() -> {
            try {
                UUID videoCutId = UUID.fromString(videoCutIdStr);
                logger.info("Corte de vídeo {} processado com sucesso. Status atualizado às: {}", videoCutId, LocalDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoCutIdStr, e.getMessage());
                throw new RuntimeException(e); // Para sair do executeWithRetry
            } catch (Exception e) {
                logger.error("Erro ao processar corte de vídeo com ID '{}'. Detalhes: {}", videoCutIdStr, e.getMessage());
                throw new RuntimeException(e); // Para sair do executeWithRetry
            }
        });
    }
}