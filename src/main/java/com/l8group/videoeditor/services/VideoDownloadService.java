package com.l8group.videoeditor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.exceptions.ProcessedFileNotFoundException;
import com.l8group.videoeditor.exceptions.VideoProcessingNotFoundException;
import com.l8group.videoeditor.metrics.VideoDownloadMetrics;
import com.l8group.videoeditor.models.VideoProcessingBatch;
import com.l8group.videoeditor.repositories.VideoBatchProcessRepository;

import io.micrometer.core.instrument.Timer;

@Service
public class VideoDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadService.class);

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    private final VideoBatchProcessRepository videoBatchProcessRepository;
    private final VideoDownloadMetrics videoDownloadMetrics;

    public VideoDownloadService(VideoBatchProcessRepository videoBatchProcessRepository, VideoDownloadMetrics videoDownloadMetrics) {
        this.videoBatchProcessRepository = videoBatchProcessRepository;
        this.videoDownloadMetrics = videoDownloadMetrics;
    }

    public Resource getProcessedVideo(UUID batchProcessId) {
        videoDownloadMetrics.incrementDownloadRequests();

        Timer.Sample downloadTimer = videoDownloadMetrics.startDownloadTimer();

        VideoProcessingBatch batchProcess = videoBatchProcessRepository.findById(batchProcessId)
                .orElseThrow(() -> {
                    logger.error("Processamento em lote {} não encontrado.", batchProcessId);
                    videoDownloadMetrics.incrementFailedDownloads();
                    return new VideoProcessingNotFoundException("Processamento em lote não encontrado.");
                });

        String videoFilePath = batchProcess.getVideoFilePath();

        try {
            // Verifica se o caminho é um URL (S3) ou um caminho local
            if (videoFilePath.startsWith("http://") || videoFilePath.startsWith("https://")) {
                return downloadFromS3(videoFilePath, batchProcess.getId());
            } else {
                return downloadFromLocal(videoFilePath);
            }
        } catch (IOException e) {
            logger.error("Erro ao baixar o arquivo processado.", e);
            videoDownloadMetrics.incrementFailedDownloads();
            throw new ProcessedFileNotFoundException("Erro ao baixar o arquivo processado.");
        } finally {
            videoDownloadMetrics.recordDownloadDuration(downloadTimer);
        }
    }

    private Resource downloadFromLocal(String localFilePath) {
        Path filePath = Paths.get(localFilePath);
        File file = filePath.toFile();

        if (!file.exists()) {
            logger.error("Arquivo processado não encontrado: {}", localFilePath);
            videoDownloadMetrics.incrementFailedDownloads();
            throw new ProcessedFileNotFoundException("Arquivo processado não encontrado.");
        }

        long fileSize = file.length();
        videoDownloadMetrics.addDownloadedFileSize(fileSize);
        videoDownloadMetrics.incrementSuccessfulDownloads();

        logger.info("Download realizado com sucesso: {}", localFilePath);
        return new FileSystemResource(file);
    }

    private Resource downloadFromS3(String s3Url, UUID batchProcessId) throws IOException {
        URL url = new URL(s3Url);
        String tempFileName = batchProcessId + "_" + Paths.get(url.getPath()).getFileName().toString();
        Path tempFilePath = Paths.get(TEMP_DIR, tempFileName);
        File tempFile = tempFilePath.toFile();

        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        long fileSize = tempFile.length();
        videoDownloadMetrics.addDownloadedFileSize(fileSize);
        videoDownloadMetrics.incrementSuccessfulDownloads();

        logger.info("Download realizado com sucesso do S3: {}", s3Url);
        return new FileSystemResource(tempFile);
    }
}