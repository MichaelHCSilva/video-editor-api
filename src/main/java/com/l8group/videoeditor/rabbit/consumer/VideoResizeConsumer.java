package com.l8group.videoeditor.rabbit.consumer;

import com.l8group.videoeditor.config.RabbitMQConfig;
import com.l8group.videoeditor.enums.VideoStatusEnum;
//import com.l8group.videoeditor.models.VideoResize;
import com.l8group.videoeditor.repositories.VideoResizeRepository;
import com.l8group.videoeditor.services.VideoStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class VideoResizeConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoResizeConsumer.class);

    private final VideoResizeRepository videoResizeRepository;
    private final VideoStatusService videoStatusService;

    @Autowired
    public VideoResizeConsumer(VideoResizeRepository videoResizeRepository, VideoStatusService videoStatusService) {
        this.videoResizeRepository = videoResizeRepository;
        this.videoStatusService = videoStatusService;
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_RESIZE_QUEUE)
    public void processVideoResize(String videoResizeIdStr) {
        executeWithRetry(() -> {
            try {
                UUID videoResizeId = UUID.fromString(videoResizeIdStr);
                videoStatusService.updateVideoStatus(videoResizeRepository, videoResizeId, VideoStatusEnum.COMPLETED);
                logger.info("Redimensionamento de vídeo {} processado com sucesso. Status atualizado às: {}", videoResizeId, ZonedDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoResizeIdStr, e.getMessage());
                updateStatusToError(videoResizeIdStr, "Erro ao converter UUID");
                throw new RuntimeException(e); // Para sair do executeWithRetry
            } catch (Exception e) {
                logger.error("Erro ao processar redimensionamento de vídeo com ID '{}'. Detalhes: {}", videoResizeIdStr, e.getMessage());
                updateStatusToError(videoResizeIdStr, "Erro durante o processamento do redimensionamento de vídeo");
                throw new RuntimeException(e); // Para sair do executeWithRetry
            }
        });
    }

    private void updateStatusToError(String videoResizeIdStr, String errorMessage) {
        try {
            UUID videoResizeId = UUID.fromString(videoResizeIdStr);
            videoStatusService.updateVideoStatus(videoResizeRepository, videoResizeId, VideoStatusEnum.ERROR);
            logger.error("Status do redimensionamento de vídeo {} atualizado para ERROR às: {}. Detalhes: {}", videoResizeId, ZonedDateTime.now(), errorMessage);
        } catch (IllegalArgumentException innerException) {
            logger.error("Erro ao converter UUID para atualizar status (String '{}'): {}", videoResizeIdStr, innerException.getMessage());
            logger.error("Não foi possível atualizar o status do redimensionamento de vídeo para ERROR devido a erro na conversão do UUID.");
        } catch (Exception innerException) {
            logger.error("Erro ao atualizar status do redimensionamento de vídeo {}: {}", videoResizeIdStr, innerException.getMessage());
        }
    }
}