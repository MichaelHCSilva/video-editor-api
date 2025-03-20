package com.l8group.videoeditor.rabbit.consumer;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.repositories.VideoOverlayRepository;
import com.l8group.videoeditor.services.VideoStatusService;

@Service
public class VideoOverlayConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoOverlayConsumer.class);

    private final VideoOverlayRepository videoOverlayRepository;
    private final VideoStatusService videoStatusService;

    @Autowired
    public VideoOverlayConsumer(VideoOverlayRepository videoOverlayRepository, VideoStatusService videoStatusService) {
        this.videoOverlayRepository = videoOverlayRepository;
        this.videoStatusService = videoStatusService;
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_OVERLAY_QUEUE)
    public void processVideoOverlay(String videoOverlayIdStr) {
        executeWithRetry(() -> {
            try {
                UUID videoOverlayId = UUID.fromString(videoOverlayIdStr);
                videoStatusService.updateVideoStatus(videoOverlayRepository, videoOverlayId, VideoStatusEnum.COMPLETED);
                logger.info("Overlay de vídeo {} processado com sucesso. Status atualizado às: {}", videoOverlayId, ZonedDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoOverlayIdStr, e.getMessage());
                updateStatusToError(videoOverlayIdStr, "Erro ao converter UUID");
                throw new RuntimeException(e); // Para sair do executeWithRetry
            } catch (Exception e) {
                logger.error("Erro ao processar overlay de vídeo com ID '{}'. Detalhes: {}", videoOverlayIdStr, e.getMessage());
                updateStatusToError(videoOverlayIdStr, "Erro durante o processamento do overlay de vídeo");
                throw new RuntimeException(e); // Para sair do executeWithRetry
            }
        });
    }

    private void updateStatusToError(String videoOverlayIdStr, String errorMessage) {
        try {
            UUID videoOverlayId = UUID.fromString(videoOverlayIdStr);
            videoStatusService.updateVideoStatus(videoOverlayRepository, videoOverlayId, VideoStatusEnum.ERROR);
            logger.error("Status do overlay de vídeo {} atualizado para ERROR às: {}. Detalhes: {}", videoOverlayId, ZonedDateTime.now(), errorMessage);
        } catch (IllegalArgumentException innerException) {
            logger.error("Erro ao converter UUID para atualizar status (String '{}'): {}", videoOverlayIdStr, innerException.getMessage());
            logger.error("Não foi possível atualizar o status do overlay de vídeo para ERROR devido a erro na conversão do UUID.");
        } catch (Exception innerException) {
            logger.error("Erro ao atualizar status do overlay de vídeo {}: {}", videoOverlayIdStr, innerException.getMessage());
        }
    }
}