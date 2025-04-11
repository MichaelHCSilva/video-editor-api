package com.l8group.videoeditor.rabbit.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;

@Service
public class VideoResizeProducer {

    private static final Logger logger = LoggerFactory.getLogger(VideoResizeProducer.class);
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public VideoResizeProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(String videoResizeId) {
        logger.info("[VideoResizeProducer] Enviando mensagem de redimensionamento de vídeo para o RabbitMQ para o VideoResize ID: {}", videoResizeId);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_EXCHANGE, RabbitMQConfig.VIDEO_RESIZE_ROUTING_KEY, videoResizeId);
            logger.info("[VideoResizeProducer] Mensagem de redimensionamento de vídeo enviada para o RabbitMQ.");
        } catch (AmqpException e) { 
            logger.error("[VideoResizeProducer] Erro ao enviar mensagem de redimensionamento de vídeo para o RabbitMQ: {}", e.getMessage(), e); 
        }
    }
}