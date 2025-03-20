package com.l8group.videoeditor.rabbit.producer;


import com.l8group.videoeditor.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class VideoOverlayProducer {

    private final RabbitTemplate rabbitTemplate;
    private static final Logger logger = LoggerFactory.getLogger(VideoOverlayProducer.class);

    public VideoOverlayProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendVideoOverlayMessage(String videoOverlayId) {
        logger.info("Enviando mensagem de overlay de vídeo para o RabbitMQ para o VideoOverlay ID: {}", videoOverlayId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_EXCHANGE, RabbitMQConfig.VIDEO_OVERLAY_ROUTING_KEY, videoOverlayId);
    }
}