package com.l8group.videoeditor.rabbit.producer;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.l8group.videoeditor.config.RabbitMQConfig;
import com.l8group.videoeditor.requests.VideoBatchRequest;

@Component
public class VideoBatchProducer {

    private final RabbitTemplate rabbitTemplate;
    private static final Logger logger = LoggerFactory.getLogger(VideoBatchProducer.class);

    @Autowired
    public VideoBatchProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendBatchMessage(VideoBatchRequest videoBatchRequest) {
        logger.info("🚀 [VideoBatchProducer] Enviando mensagem de processamento em lote para o RabbitMQ: {}", videoBatchRequest);
        rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_EXCHANGE, RabbitMQConfig.VIDEO_BATCH_PROCESSING_ROUTING_KEY, videoBatchRequest);
        logger.info("✅ [VideoBatchProducer] Mensagem de processamento em lote enviada para o RabbitMQ.");
    }

    public void sendVideoBatchId(UUID batchId) {
        logger.info("🚀 [VideoBatchProducer] Enviando ID do lote para processamento: {}", batchId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_EXCHANGE, RabbitMQConfig.VIDEO_BATCH_PROCESSING_ROUTING_KEY, batchId.toString());
        logger.info("✅ [VideoBatchProducer] ID do lote enviado para processamento.");
    }
}