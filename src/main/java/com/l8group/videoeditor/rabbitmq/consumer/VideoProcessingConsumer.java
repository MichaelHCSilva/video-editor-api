package com.l8group.videoeditor.rabbitmq.consumer;

import com.l8group.videoeditor.config.RabbitMQConfig;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.services.StatusUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoProcessingConsumer {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingConsumer.class);
    private final VideoFileRepository videoFileRepository;
    private final StatusUpdateService statusUpdateService;

    public VideoProcessingConsumer(VideoFileRepository videoFileRepository, StatusUpdateService statusUpdateService) {
        this.videoFileRepository = videoFileRepository;
        this.statusUpdateService = statusUpdateService;
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_UPLOAD_QUEUE)  // ✅ Corrigido: escuta a fila correta
    public void processVideo(String videoIdStr) {
        UUID videoId = UUID.fromString(videoIdStr);
        log.info("📩 Recebida solicitação para processar vídeo ID: {}", videoId);

        Optional<VideoFile> videoFileOptional = videoFileRepository.findById(videoId);

        if (videoFileOptional.isEmpty()) {
            log.error("❌ Vídeo com ID {} não encontrado!", videoId);
            return;
        }

        //VideoFile videoFile = videoFileOptional.get();
        try {
            Thread.sleep(50000); // Simula o processamento de vídeo
            statusUpdateService.updateStatus(videoFileRepository, videoId, VideoStatus.COMPLETED);
            log.info("✅ Processamento concluído para vídeo ID: {}", videoId);
        } catch (Exception e) {
            log.error("🔥 Erro ao processar vídeo ID {}: {}", videoId, e.getMessage());
            statusUpdateService.updateStatus(videoFileRepository, videoId, VideoStatus.ERROR);
        }
    }
}
