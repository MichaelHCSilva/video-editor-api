package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dtos.VideoFileResponseDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoFileRequest;
import com.l8group.videoeditor.utils.VideoDuration;

@Service
public class VideoFileService {

    private static final Logger logger = LoggerFactory.getLogger(VideoFileService.class);

    private final VideoFileRepository videoFileRepository;
    private final VideoValidationService validationService;

    private final String uploadDir = "/mnt/c/Users/micha/OneDrive/Documentos/video-editor-api/videos/";

    public VideoFileService(VideoFileRepository videoFileRepository, VideoValidationService validationService) {
        this.videoFileRepository = videoFileRepository;
        this.validationService = validationService;
    }

    public List<UUID> uploadVideo(List<VideoFileRequest> videoFileRequests, List<String> rejectedFiles) {
        List<UUID> processedFiles = new ArrayList<>();

        for (VideoFileRequest request : videoFileRequests) {
            MultipartFile file = request.getFile();
            String fileFormat = validationService.getFileExtension(file.getOriginalFilename());

            if (!validationService.isSupportedFormat(fileFormat)) {
                rejectedFiles.add(file.getOriginalFilename() + " (Formato inválido)");
                continue;
            }

            try {
                String filePath = saveFile(file);

                VideoFile videoFile = new VideoFile();
                videoFile.setFileName(file.getOriginalFilename());
                videoFile.setFileSize(file.getSize());
                videoFile.setFileFormat(fileFormat);
                videoFile.setUploadedAt(ZonedDateTime.now());
                videoFile.setStatus(VideoStatus.PROCESSING);
                videoFile.setFilePath(filePath);

                Long duration = VideoDuration.getVideoDurationInSeconds(filePath);
                videoFile.setDuration(duration);

                videoFileRepository.save(videoFile);
                processedFiles.add(videoFile.getId());

                logger.info("Arquivo processado: {}", file.getOriginalFilename());
            } catch (IOException | InterruptedException e) {
                logger.error("Erro ao processar o arquivo {}: {}", file.getOriginalFilename(), e.getMessage());
                rejectedFiles.add(file.getOriginalFilename() + " (Erro ao salvar ou processar)");
            }
        }

        return processedFiles;
    }

    public List<VideoFileResponseDTO> getAllVideos() {
        List<VideoFile> videoFiles = videoFileRepository.findAll();
        List<VideoFileResponseDTO> videoFileDTOs = new ArrayList<>();

        for (VideoFile videoFile : videoFiles) {
            videoFileDTOs.add(new VideoFileResponseDTO(
                videoFile.getFileName(),
                videoFile.getUploadedAt(),
                videoFile.getStatus()
            ));
        }

        return videoFileDTOs;
    }

    private String saveFile(MultipartFile file) throws IOException {
        String uniqueFileName = UUID.randomUUID() + "." + validationService.getFileExtension(file.getOriginalFilename());
        File directory = new File(uploadDir);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        String filePath = uploadDir + uniqueFileName;
        file.transferTo(new File(filePath));

        return filePath;
    }
}
