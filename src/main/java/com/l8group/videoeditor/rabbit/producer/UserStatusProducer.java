package com.l8group.videoeditor.rabbit.producer;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

@Service
public class UserStatusProducer {

    private static final Logger logger = LoggerFactory.getLogger(UserStatusProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendUserStatusUpdate(UUID userId, VideoStatusEnum newStatus) {
        logger.info("Enviando solicitação de atualização de status para o usuário com ID '{}' para '{}'", userId, newStatus);

        Message message = MessageBuilder
                .withBody(userId.toString().getBytes())
                .setHeader("newStatus", newStatus.toString())
                .build();

        // Envia para a Exchange com a routing key correta
        rabbitTemplate.send(RabbitMQConfig.VIDEO_EXCHANGE, RabbitMQConfig.USER_STATUS_ROUTING_KEY, message);

        logger.info("Mensagem de atualização de status enviada para a exchange '{}' com a routing key '{}' para o usuário '{}'",
                RabbitMQConfig.VIDEO_EXCHANGE, RabbitMQConfig.USER_STATUS_ROUTING_KEY, userId);
    }
}