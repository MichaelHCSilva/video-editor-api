package com.l8group.videoeditor.services;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dto.VideoFileDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.utils.FFmpegUtils;

@Service
public class VideoUploadService {

    private static final Logger logger = LoggerFactory.getLogger(VideoUploadService.class);

    @Autowired
    private VideoFileRepository videoFileRepository;

    @Autowired
    private FFmpegUtils ffmpegUtils;

    public UUID uploadVideo(MultipartFile file) {
        logger.info("Iniciando processamento do vídeo...");

        validarArquivo(file);

        VideoFile videoFile = criarVideoFile(file);

        long duration = ffmpegUtils.getVideoDuration(file);
        videoFile.setDuration(duration);

        VideoFile savedFile = videoFileRepository.save(videoFile);
        logger.info("Vídeo salvo com sucesso. ID: {}", savedFile.getId());

        return savedFile.getId();
    }

    public List<VideoFileDTO> listarVideos() {
        return videoFileRepository.findFileNamesAndUploadDates()
                .stream()
                .map(this::mapToVideoFileDTO)
                .collect(Collectors.toList());
    }

    private void validarArquivo(MultipartFile file) {
        if (file.isEmpty()) {
            logger.warn("Arquivo vazio.");
            throw new IllegalArgumentException("Nenhum arquivo enviado.");
        }

        String contentType = file.getContentType();
        if (!isValidVideoFormat(contentType)) {
            logger.warn("Formato de vídeo inválido: {}", contentType);
            throw new IllegalArgumentException("Formato de vídeo inválido! Envie MP4, AVI ou MOV.");
        }
    }

    private VideoFile criarVideoFile(MultipartFile file) {
        VideoFile videoFile = new VideoFile();
        videoFile.setFileName(file.getOriginalFilename());
        videoFile.setFileSize(file.getSize());
        videoFile.setFileFormat(file.getContentType());
        videoFile.setUploadedAt(ZonedDateTime.now());
        videoFile.setStatus(VideoStatus.PROCESSING);

        return videoFile;
    }

    private VideoFileDTO mapToVideoFileDTO(Object[] videoDetails) {
        return new VideoFileDTO(
                (String) videoDetails[0],
                (ZonedDateTime) videoDetails[1],
                (VideoStatus) videoDetails[2],
                (Long) videoDetails[3]
        );
    }

    private boolean isValidVideoFormat(String contentType) {
        return contentType != null &&
                (contentType.equals("video/mp4") ||
                        contentType.equals("video/x-msvideo") ||
                        contentType.equals("video/quicktime"));
    }
}
