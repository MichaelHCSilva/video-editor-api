package com.l8group.videoeditor.services;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.models.VideoBatchProcess;
import com.l8group.videoeditor.repositories.VideoBatchProcessRepository;

@Service
public class VideoDownloadService {

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    private final VideoBatchProcessRepository videoBatchProcessRepository;

    public VideoDownloadService(VideoBatchProcessRepository videoBatchProcessRepository) {
        this.videoBatchProcessRepository = videoBatchProcessRepository;
    }

    public Resource getProcessedVideo(UUID batchProcessId) {
        VideoBatchProcess batchProcess = videoBatchProcessRepository.findById(batchProcessId)
                .orElseThrow(() -> new RuntimeException("Processamento em lote não encontrado."));

        String processedFileName = batchProcess.getProcessedFileName();

        Path processedFilePath = Paths.get(TEMP_DIR, processedFileName);

        File file = processedFilePath.toFile();
        if (!file.exists()) {
            throw new RuntimeException("Arquivo processado não encontrado.");
        }

        return new FileSystemResource(file);
    }
}
