package com.l8group.videoeditor.services;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dtos.VideoFileDTO;
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

    public List<UUID> uploadVideos(MultipartFile[] files, List<String> rejectedFiles) {
        List<UUID> videoIds = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                UUID videoId = uploadVideo(file);
                videoIds.add(videoId);
            } catch (IllegalArgumentException e) {
                rejectedFiles.add(file.getOriginalFilename());
                logger.warn("Arquivo rejeitado: {}. Motivo: {}", file.getOriginalFilename(), e.getMessage());
            }
        }

        return videoIds;
    }

    public UUID uploadVideo(MultipartFile file) {
        String fileFormat = getFileExtension(file.getOriginalFilename());

        // Verifica se o formato é suportado
        if (!isSupportedFormat(fileFormat)) {
            throw new IllegalArgumentException("Formato inválido. Somente os formatos mp4, avi e mov são permitidos.");
        }

        // Verifica se o arquivo está vazio
        if (file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo está vazio ou corrompido.");
        }

        // Valida o conteúdo do vídeo
        if (!isValidVideoContent(file)) {
            throw new IllegalArgumentException("O vídeo está corrompido ou ilegível.");
        }

        VideoFile videoFile = new VideoFile();
        videoFile.setFileName(file.getOriginalFilename());
        videoFile.setFileSize(file.getSize());
        videoFile.setFileFormat(fileFormat);
        videoFile.setUploadedAt(ZonedDateTime.now());
        videoFile.setStatus(VideoStatus.PROCESSING);

        VideoFile savedVideo = videoFileRepository.save(videoFile);
        logger.info("Metadados do vídeo salvos com sucesso para o arquivo: {}", file.getOriginalFilename());

        return savedVideo.getId();
    }

    private boolean isValidVideoContent(MultipartFile file) {
        try {
            return file.getSize() > 0;
        } catch (Exception e) {
            logger.error("Erro ao validar conteúdo do vídeo: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Erro ao validar o conteúdo do vídeo.");
        }
    }

    private boolean isSupportedFormat(String fileFormat) {
        // Permite somente os formatos 'avi', 'mp4' e 'mov'
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

    public List<VideoFileDTO> getAllVideos() {
        return videoFileRepository.findAllVideos();
    }
}
