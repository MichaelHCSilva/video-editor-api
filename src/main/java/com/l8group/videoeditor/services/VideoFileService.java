package com.l8group.videoeditor.services;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;

@Service
public class VideoFileService {

    private static final Logger logger = LoggerFactory.getLogger(VideoFileService.class);

    private final VideoFileRepository videoFileRepository;

    public VideoFileService(VideoFileRepository videoFileRepository) {
        this.videoFileRepository = videoFileRepository;
    }

    public List<UUID> uploadVideos(MultipartFile[] files) {
        List<UUID> videoIds = new ArrayList<>();
        for (MultipartFile file : files) {
            UUID videoId = uploadVideo(file);
            videoIds.add(videoId);
        }
        return videoIds;
    }

    @Async
    public CompletableFuture<UUID> uploadVideoAsync(MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> uploadVideo(file));
    }

    public UUID uploadVideo(MultipartFile file) {
        String fileFormat = getFileExtension(file.getOriginalFilename());
        if (!isSupportedFormat(fileFormat)) {
            logger.warn("Formato de arquivo não suportado: {}", fileFormat);
            throw new IllegalArgumentException("Formato de arquivo não suportado.");
        }

        if (file.isEmpty()) {
            logger.warn("Arquivo vazio ou corrompido: {}", file.getOriginalFilename());
            throw new IllegalArgumentException("O arquivo está vazio ou corrompido.");
        }

        String originalFileName = file.getOriginalFilename();

        if (!isValidVideoContent(file)) {
            logger.warn("Vídeo inválido ou corrompido: {}", originalFileName);
            throw new IllegalArgumentException("O vídeo está corrompido ou ilegível.");
        }

        VideoFile videoFile = new VideoFile();
        videoFile.setFileName(originalFileName);
        videoFile.setFileSize(file.getSize());
        videoFile.setFileFormat(fileFormat);
        videoFile.setUploadedAt(ZonedDateTime.now());
        videoFile.setStatus(VideoStatus.PROCESSING);

        VideoFile savedVideo = videoFileRepository.save(videoFile);
        logger.info("Metadados do vídeo salvos com sucesso para o arquivo: {}", originalFileName);

        return savedVideo.getId();
    }

    private boolean isValidVideoContent(MultipartFile file) {
        try {

            if (file.getSize() > 0) {
                return true;
            } else {
                logger.warn("O arquivo está vazio ou corrompido.");
                return false;
            }

        } catch (Exception e) {
            logger.error("Erro ao validar conteúdo do vídeo.", e);
            return false;
        }
    }

    private boolean isSupportedFormat(String fileFormat) {
        return fileFormat.equalsIgnoreCase("mp4")
                || fileFormat.equalsIgnoreCase("avi")
                || fileFormat.equalsIgnoreCase("mov");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            logger.error("Nome do arquivo inválido: {}", fileName);
            throw new IllegalArgumentException("Nome do arquivo inválido.");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
