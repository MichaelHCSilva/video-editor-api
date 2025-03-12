package com.l8group.videoeditor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.exceptions.ProcessedFileNotFoundException;
import com.l8group.videoeditor.exceptions.VideoProcessingNotFoundException;
import com.l8group.videoeditor.models.VideoProcessingBatch;
import com.l8group.videoeditor.repositories.VideoBatchProcessRepository;

@Service
public class VideoDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadService.class);

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    private final VideoBatchProcessRepository videoBatchProcessRepository;

    public VideoDownloadService(VideoBatchProcessRepository videoBatchProcessRepository) {
        this.videoBatchProcessRepository = videoBatchProcessRepository;
    }

    public Resource getProcessedVideo(UUID batchProcessId) {
        VideoProcessingBatch batchProcess = videoBatchProcessRepository.findById(batchProcessId)
                .orElseThrow(() -> {
                    logger.error("Processamento em lote {} não encontrado.", batchProcessId);
                    return new VideoProcessingNotFoundException("Processamento em lote não encontrado.");
                });

        String processedFileName = batchProcess.getVideoOutputFileName();
        Path processedFilePath = Paths.get(TEMP_DIR, processedFileName);
        File file = processedFilePath.toFile();

        if (!file.exists()) {
            logger.error("Arquivo processado não encontrado: {}", processedFileName);
            throw new ProcessedFileNotFoundException("Arquivo processado não encontrado.");
        }

        logger.info("Download realizado com sucesso: {}", processedFileName);
        return new FileSystemResource(file);
    }
}
