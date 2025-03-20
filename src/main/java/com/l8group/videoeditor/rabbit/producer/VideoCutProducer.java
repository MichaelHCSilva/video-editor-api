package com.l8group.videoeditor.rabbit.producer;

import com.l8group.videoeditor.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VideoCutProducer {

    private static final Logger logger = LoggerFactory.getLogger(VideoCutProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public VideoCutProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendVideoCutId(UUID videoCutId) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_EXCHANGE, RabbitMQConfig.VIDEO_CUT_ROUTING_KEY, videoCutId.toString());
            logger.info("Mensagem de corte de vídeo enviada para o RabbitMQ para o VideoCut ID: {}", videoCutId);
        } catch (Exception e) {
            logger.error("Erro ao enviar mensagem de corte de vídeo para o RabbitMQ para o VideoCut ID: {}: {}", videoCutId, e.getMessage());
        }
    }
}