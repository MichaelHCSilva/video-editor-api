package com.l8group.videoeditor.rabbitmq.producer;

import com.l8group.videoeditor.config.RabbitMQConfig;
import com.l8group.videoeditor.models.VideoFile;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class VideoProcessingProducer {

    private final RabbitTemplate rabbitTemplate;

    public VideoProcessingProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendVideoForProcessing(VideoFile videoFile) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.VIDEO_EXCHANGE,  // ✅ Corrigido: Envia para a exchange correta
            RabbitMQConfig.VIDEO_UPLOAD_ROUTING_KEY,  // ✅ Usa a routing key correta
            videoFile.getId().toString()
        );
    }
}
