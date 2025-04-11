package com.l8group.videoeditor.rabbit.producer;

import com.l8group.videoeditor.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class VideoConversionProducer {

    private final RabbitTemplate rabbitTemplate;
    private static final Logger logger = LoggerFactory.getLogger(VideoConversionProducer.class);

    public VideoConversionProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendVideoConversionMessage(String videoConversionId) {
        logger.info("[VideoConversionProducer] Enviando mensagem de conversão de vídeo para o RabbitMQ para o VideoConversion ID: {}", videoConversionId);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_EXCHANGE, RabbitMQConfig.VIDEO_CONVERSION_ROUTING_KEY, videoConversionId);
            logger.info("[VideoConversionProducer] Mensagem de conversão de vídeo enviada para o RabbitMQ.");
        } catch (AmqpException e) {
            logger.error("[VideoConversionProducer] Erro ao enviar mensagem de conversão de vídeo para o RabbitMQ: {}", e.getMessage(), e);
        }
    }
}