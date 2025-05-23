package com.l8group.videoeditor.rabbit.producer;

import com.l8group.videoeditor.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component; 

import java.util.UUID;

@Component 
public class VideoDownloadProducer {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadProducer.class);
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public VideoDownloadProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendDownloadId(UUID downloadId) {
        logger.info("[VideoDownloadProducer] Enviando ID de download para o RabbitMQ para o Download ID: {}", downloadId);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_EXCHANGE, RabbitMQConfig.VIDEO_DOWNLOAD_ROUTING_KEY, downloadId.toString());
            logger.info("[VideoDownloadProducer] ID de download enviado com sucesso para o RabbitMQ para o Download ID: {}", downloadId);
        } catch (AmqpException e) {
            logger.error("[VideoDownloadProducer] Erro ao enviar ID de download para o RabbitMQ para o Download ID: {}: {}", downloadId, e.getMessage(), e);

        }
    }
}