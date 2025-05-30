package com.l8group.videoeditor.rabbit.producer;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException; 
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;

@Service
public class VideoProcessingProducer {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingProducer.class);
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public VideoProcessingProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendVideoId(UUID videoId) {
        logger.info("[VideoProcessingProducer] Enviando mensagem para o RabbitMQ para o VideoFile {}.", videoId);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_EXCHANGE, RabbitMQConfig.VIDEO_PROCESSING_ROUTING_KEY, videoId.toString());
            logger.info("[VideoProcessingProducer] Mensagem enviada para o RabbitMQ para o VideoFile {}.", videoId);
        } catch (AmqpException e) { 
            logger.error("[VideoProcessingProducer] Erro ao enviar mensagem para o RabbitMQ para o VideoFile {}: {}", videoId, e.getMessage(), e); 
        }
    }
}