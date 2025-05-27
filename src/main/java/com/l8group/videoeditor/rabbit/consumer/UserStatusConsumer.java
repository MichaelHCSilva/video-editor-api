package com.l8group.videoeditor.rabbit.consumer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;

@Service
public class UserStatusConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(UserStatusConsumer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public UserStatusConsumer() {}

    @RabbitListener(queues = RabbitMQConfig.USER_STATUS_QUEUE)
    public void processUserStatus(@Payload String userIdStr, Message<?> message) {
        executeWithRetry(() -> {
            try {
                UUID userId = UUID.fromString(userIdStr);
                logger.info("Status de usuário {} processado com sucesso. Status atualizado às: {}", userId, LocalDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", userIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.USER_STATUS_DLQ, userIdStr);
                throw new RuntimeException("Erro na conversão do UUID do usuário: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Erro ao processar status do usuário com ID '{}'. Detalhes: {}", userIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.USER_STATUS_DLQ, userIdStr);
                throw new RuntimeException("Erro inesperado no processamento do status do usuário: " + e.getMessage(), e);
            }
        });
    }
}
