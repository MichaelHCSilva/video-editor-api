package com.l8group.videoeditor.rabbit.producer;

import com.l8group.videoeditor.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VideoResizeProducer {

    private static final Logger logger = LoggerFactory.getLogger(VideoResizeProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public VideoResizeProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(String videoResizeId) {
        logger.info("Enviando mensagem de redimensionamento de vídeo para o RabbitMQ para o VideoResize ID: {}", videoResizeId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_EXCHANGE, RabbitMQConfig.VIDEO_RESIZE_ROUTING_KEY, videoResizeId);
    }
}