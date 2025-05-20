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
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.services.VideoStatusManagerService;

@Service
public class UserStatusConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(UserStatusConsumer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private VideoStatusManagerService videoStatusManagerService;

    @Autowired
    private com.l8group.videoeditor.repositories.UserRepository userRepository;

    public UserStatusConsumer() {
    }

    @RabbitListener(queues = RabbitMQConfig.USER_STATUS_QUEUE)
    public void processUserStatusUpdate(@Payload String userIdStr, Message<?> message) {
        executeWithRetry(() -> {
            try {
                UUID userId = UUID.fromString(userIdStr);
                logger.info("Solicitação de atualização de status recebida para o usuário com ID '{}' às: {}", userId, LocalDateTime.now());

                String newStatusStr = message.getHeaders().get("newStatus", String.class);
                if (newStatusStr == null) {
                    logger.error("Header 'newStatus' não encontrado na mensagem para o usuário com ID: {}", userId);
                    rabbitTemplate.convertAndSend(RabbitMQConfig.USER_STATUS_DLQ, userIdStr);
                    throw new IllegalArgumentException("Header 'newStatus' ausente na mensagem.");
                }
                VideoStatusEnum newStatus = VideoStatusEnum.valueOf(newStatusStr);

                videoStatusManagerService.updateEntityStatus(userRepository, userId, newStatus, "RabbitMQ Consumer");

                logger.info("Status do usuário '{}' atualizado para '{}' com sucesso às: {}", userId, newStatus, LocalDateTime.now());

            } catch (IllegalArgumentException e) {
                logger.error("Erro ao processar atualização de status para o usuário com ID '{}': {}", userIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.USER_STATUS_DLQ, userIdStr);
                throw new RuntimeException("Erro na atualização de status do usuário: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Erro inesperado ao processar atualização de status para o usuário com ID '{}': {}", userIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.USER_STATUS_DLQ, userIdStr);
                throw new RuntimeException("Erro inesperado na atualização de status do usuário: " + e.getMessage(), e);
            }
        });
    }
}