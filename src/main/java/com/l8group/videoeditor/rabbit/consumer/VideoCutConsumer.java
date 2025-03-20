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
import com.l8group.videoeditor.repositories.VideoCutRepository;
import com.l8group.videoeditor.services.VideoStatusService;

@Service
public class VideoCutConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoCutConsumer.class);

    private final VideoCutRepository videoCutRepository;
    private final VideoStatusService videoStatusService;

    @Autowired
    public VideoCutConsumer(VideoCutRepository videoCutRepository, VideoStatusService videoStatusService) {
        this.videoCutRepository = videoCutRepository;
        this.videoStatusService = videoStatusService;
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_CUT_QUEUE)
    public void processVideoCut(String videoCutIdStr) {
        executeWithRetry(() -> {
            try {
                UUID videoCutId = UUID.fromString(videoCutIdStr);
                videoStatusService.updateVideoStatus(videoCutRepository, videoCutId, VideoStatusEnum.COMPLETED);
                logger.info("Corte de vídeo {} processado com sucesso. Status atualizado às: {}", videoCutId, LocalDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoCutIdStr, e.getMessage());
                updateStatusToError(videoCutIdStr, "Erro ao converter UUID");
                throw new RuntimeException(e); // Para sair do executeWithRetry
            } catch (Exception e) {
                logger.error("Erro ao processar corte de vídeo com ID '{}'. Detalhes: {}", videoCutIdStr, e.getMessage());
                updateStatusToError(videoCutIdStr, "Erro durante o processamento do corte de vídeo");
                throw new RuntimeException(e); // Para sair do executeWithRetry
            }
        });
    }

    private void updateStatusToError(String videoCutIdStr, String errorMessage) {
        try {
            UUID videoCutId = UUID.fromString(videoCutIdStr);
            videoStatusService.updateVideoStatus(videoCutRepository, videoCutId, VideoStatusEnum.ERROR);
            logger.error("Status do corte de vídeo {} atualizado para ERROR às: {}. Detalhes: {}", videoCutId, LocalDateTime.now(), errorMessage);
        } catch (IllegalArgumentException innerException) {
            logger.error("Erro ao converter UUID para atualizar status (String '{}'): {}", videoCutIdStr, innerException.getMessage());
            logger.error("Não foi possível atualizar o status do corte de vídeo para ERROR devido a erro na conversão do UUID.");
        } catch (Exception innerException) {
            logger.error("Erro ao atualizar status do corte de vídeo {}: {}", videoCutIdStr, innerException.getMessage());
        }
    }
}