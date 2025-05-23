package com.l8group.videoeditor.rabbit.consumer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.config.RabbitMQConfig;
import com.l8group.videoeditor.enums.VideoStatusEnum; // Importar o enum
import com.l8group.videoeditor.repositories.VideoDownloadRepository; // Importar o repositório
import com.l8group.videoeditor.services.VideoStatusManagerService; // Importar o serviço de gerenciamento de status
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Service
public class VideoDownloadConsumer extends AbstractRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadConsumer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired 
    private VideoStatusManagerService videoStatusManagerService;

    @Autowired 
    private VideoDownloadRepository videoDownloadRepository;

    public VideoDownloadConsumer() {
        
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_DOWNLOAD_QUEUE)
    public void processVideoDownload(@Payload String downloadIdStr, Message<?> message) {
        executeWithRetry(() -> {
            UUID downloadId = null; 
            try {
                downloadId = UUID.fromString(downloadIdStr);

                logger.info("Iniciando processamento assíncrono final para download com ID: {}", downloadId);

                videoStatusManagerService.updateEntityStatus(
                    videoDownloadRepository,
                    downloadId,
                    VideoStatusEnum.COMPLETED, 
                    "VideoDownloadConsumer"
                );

                logger.info("Download de vídeo com ID {} processado com sucesso e status atualizado para COMPLETED. Última atualização: {}", downloadId, LocalDateTime.now());

            } catch (IllegalArgumentException e) {
                logger.error("Erro ao converter UUID para evento de download: String '{}' não é um UUID válido. Detalhes: {}", downloadIdStr, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_DOWNLOAD_DLQ, downloadIdStr);
                throw new RuntimeException("Erro irrecuperável: ID de download inválido. Mensagem movida para DLQ.", e);
            } catch (Exception e) {
                logger.error("Erro inesperado ao processar download de vídeo com ID '{}'. Detalhes: {}", downloadIdStr, e.getMessage());

                if (downloadId != null) {
                    videoStatusManagerService.updateEntityStatus(
                        videoDownloadRepository,
                        downloadId,
                        VideoStatusEnum.ERROR, 
                        "VideoDownloadConsumer"
                    );
                }

                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_DOWNLOAD_DLQ, downloadIdStr);
                throw new RuntimeException("Falha no processamento do evento de download: " + e.getMessage(), e);
            }
        });
    }
}