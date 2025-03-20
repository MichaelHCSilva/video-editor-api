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
import com.l8group.videoeditor.repositories.VideoConversionRepository;
import com.l8group.videoeditor.services.VideoStatusService;



@Service
public class VideoConversionConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoConversionConsumer.class);

    private final VideoConversionRepository videoConversionRepository;
    private final VideoStatusService videoStatusService;

    @Autowired
    public VideoConversionConsumer(VideoConversionRepository videoConversionRepository, VideoStatusService videoStatusService) {
        this.videoConversionRepository = videoConversionRepository;
        this.videoStatusService = videoStatusService;
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_CONVERSION_QUEUE)
    public void processVideoConversion(String videoConversionIdStr) {
        executeWithRetry(() -> {
            try {
                UUID videoConversionId = UUID.fromString(videoConversionIdStr);
                videoStatusService.updateVideoStatus(videoConversionRepository, videoConversionId, VideoStatusEnum.COMPLETED);
                logger.info("Conversão de vídeo {} processada com sucesso. Status atualizado às: {}", videoConversionId, ZonedDateTime.now());
            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoConversionIdStr, e.getMessage());
                updateStatusToError(videoConversionIdStr, "Erro ao converter UUID");
                throw new RuntimeException(e); // Para sair do executeWithRetry
            } catch (Exception e) {
                logger.error("Erro ao processar conversão de vídeo com ID '{}'. Detalhes: {}", videoConversionIdStr, e.getMessage());
                updateStatusToError(videoConversionIdStr, "Erro durante o processamento da conversão de vídeo");
                throw new RuntimeException(e); // Para sair do executeWithRetry
            }
        });
    }

    private void updateStatusToError(String videoConversionIdStr, String errorMessage) {
        try {
            UUID videoConversionId = UUID.fromString(videoConversionIdStr);
            videoStatusService.updateVideoStatus(videoConversionRepository, videoConversionId, VideoStatusEnum.ERROR);
            logger.error("Status da conversão de vídeo {} atualizado para ERROR às: {}. Detalhes: {}", videoConversionId, ZonedDateTime.now(), errorMessage);
        } catch (IllegalArgumentException innerException) {
            logger.error("Erro ao converter UUID para atualizar status (String '{}'): {}", videoConversionIdStr, innerException.getMessage());
            logger.error("Não foi possível atualizar o status da conversão de vídeo para ERROR devido a erro na conversão do UUID.");
        } catch (Exception innerException) {
            logger.error("Erro ao atualizar status da conversão de vídeo {}: {}", videoConversionIdStr, innerException.getMessage());
        }
    }
}