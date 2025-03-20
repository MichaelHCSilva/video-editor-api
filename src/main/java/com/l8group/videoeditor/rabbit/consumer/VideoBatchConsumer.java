package com.l8group.videoeditor.rabbit.consumer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.repositories.VideoBatchProcessRepository;
import com.l8group.videoeditor.services.VideoStatusService;

@Service
public class VideoBatchConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoBatchConsumer.class);

    private final VideoBatchProcessRepository videoBatchProcessRepository;
    private final VideoStatusService videoStatusService;

    @Autowired
    public VideoBatchConsumer(
            VideoBatchProcessRepository videoBatchProcessRepository,
            VideoStatusService videoStatusService) {

        this.videoBatchProcessRepository = videoBatchProcessRepository;
        this.videoStatusService = videoStatusService;
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_BATCH_PROCESSING_QUEUE)
    public void processVideoBatch(String batchIdStr) {
        executeWithRetry(() -> {
            try {
                UUID batchId = UUID.fromString(batchIdStr);
                videoStatusService.updateVideoStatus(videoBatchProcessRepository, batchId, VideoStatusEnum.COMPLETED);
                logger.info("Processamento em lote {} processado com sucesso. Status atualizado às: {}", batchId,
                        LocalDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", batchIdStr,
                        e.getMessage());
                updateStatusToError(batchIdStr, "Erro ao converter UUID");
                throw new RuntimeException(e); // Para sair do executeWithRetry
            } catch (Exception e) {
                logger.error("Erro ao processar lote de vídeos com ID '{}'. Detalhes: {}", batchIdStr, e.getMessage());
                updateStatusToError(batchIdStr, "Erro durante o processamento do lote de vídeos");
                throw new RuntimeException(e); // Para sair do executeWithRetry
            }
        });
    }

    private void updateStatusToError(String batchIdStr, String errorMessage) {
        try {
            UUID batchId = UUID.fromString(batchIdStr);
            videoStatusService.updateVideoStatus(videoBatchProcessRepository, batchId, VideoStatusEnum.ERROR);
            logger.error("Status do lote de vídeos {} atualizado para ERROR às: {}. Detalhes: {}", batchId,
                    LocalDateTime.now(), errorMessage);
        } catch (IllegalArgumentException innerException) {
            logger.error("Erro ao converter UUID para atualizar status (String '{}'): {}", batchIdStr,
                    innerException.getMessage());
            logger.error(
                    "Não foi possível atualizar o status do lote de vídeos para ERROR devido a erro na conversão do UUID.");
        } catch (Exception innerException) {
            logger.error("Erro ao atualizar status do lote de vídeos {}: {}", batchIdStr, innerException.getMessage());
        }
    }
}