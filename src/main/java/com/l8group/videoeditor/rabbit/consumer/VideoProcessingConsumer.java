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
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.services.VideoStatusService;

@Service
public class VideoProcessingConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingConsumer.class);

    private final VideoFileRepository videoFileRepository;
    private final VideoStatusService videoStatusService;

    @Autowired
    public VideoProcessingConsumer(VideoFileRepository videoFileRepository, VideoStatusService videoStatusService) {
        this.videoFileRepository = videoFileRepository;
        this.videoStatusService = videoStatusService;
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_PROCESSING_QUEUE)
    public void processVideo(String videoIdStr) {
        try {
            // Converte a string UUID diretamente
            UUID videoId = UUID.fromString(videoIdStr);
            // Atualiza o status usando VideoStatusService
            videoStatusService.updateVideoStatus(videoFileRepository, videoId, VideoStatusEnum.COMPLETED);
            logger.info("VideoFile {} processado com sucesso. Status atualizado às: {}", videoId, LocalDateTime.now());
        } catch (IllegalArgumentException e) {
            logger.error("Erro ao converter UUID: String '{}' não é um UUID válido. Detalhes: {}", videoIdStr,
                    e.getMessage());
            updateStatusToError(videoIdStr, "Erro ao converter UUID");

        } catch (Exception e) {
            logger.error("Erro ao processar VideoFile com ID '{}'. Detalhes: {}", videoIdStr, e.getMessage());
            updateStatusToError(videoIdStr, "Erro durante o processamento do vídeo");
        }
    }

    private void updateStatusToError(String videoIdStr, String errorMessage) {
        try {
            UUID videoId = UUID.fromString(videoIdStr); // Tenta converter novamente para atualizar o status
            videoStatusService.updateVideoStatus(videoFileRepository, videoId, VideoStatusEnum.ERROR);
            logger.error("Status do VideoFile {} atualizado para ERROR às: {}. Detalhes: {}", videoId,
                    LocalDateTime.now(), errorMessage);

        } catch (IllegalArgumentException innerException) {
            logger.error("Erro ao converter UUID para atualizar status (String '{}'): {}", videoIdStr,
                    innerException.getMessage());
            logger.error("Não foi possível atualizar o status do vídeo para ERROR devido a erro na conversão do UUID.");

        } catch (Exception innerException) {
            logger.error("Erro ao atualizar status do VideoFile {}: {}", videoIdStr, innerException.getMessage());
        }
    }
}