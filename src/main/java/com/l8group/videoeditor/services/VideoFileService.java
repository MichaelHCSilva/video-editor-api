package com.l8group.videoeditor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dtos.VideoFileDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.utils.VideoDuration;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VideoFileService {

    private static final Logger logger = LoggerFactory.getLogger(VideoFileService.class);

    private final VideoFileRepository videoFileRepository;

    public VideoFileService(VideoFileRepository videoFileRepository) {
        this.videoFileRepository = videoFileRepository;
    }

    public List<UUID> uploadVideo(MultipartFile[] files, List<String> rejectedFiles) {
        List<UUID> processedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileFormat = getFileExtension(file.getOriginalFilename());

            if (!isSupportedFormat(fileFormat)) {
                rejectedFiles.add(file.getOriginalFilename() + " (Formato inválido)");
                
            } else if (file.isEmpty()) {
                rejectedFiles.add(file.getOriginalFilename() + " (Arquivo vazio)");
              
            } else if (!isValidVideoContent(file)) {
                rejectedFiles.add(file.getOriginalFilename() + " (Arquivo corrompido ou ilegível)");
           
            } else {
                try {
                  
                    String filePath = saveFile(file);  

                    
                    VideoFile videoFile = new VideoFile();
                    videoFile.setFileName(file.getOriginalFilename());
                    videoFile.setFileSize(file.getSize());
                    videoFile.setFileFormat(fileFormat);
                    videoFile.setUploadedAt(ZonedDateTime.now());
                    videoFile.setStatus(VideoStatus.PROCESSING);

                    
                    Long videoDuration = VideoDuration.getVideoDurationInSeconds(filePath);
                    videoFile.setDuration(videoDuration);

                    
                    videoFileRepository.save(videoFile);
                    processedFiles.add(videoFile.getId());
                    logger.info("Metadados do vídeo salvos com sucesso para o arquivo: {}", file.getOriginalFilename());
                } catch (IOException | InterruptedException e) {
                    logger.error("Erro ao processar o arquivo {}: {}", file.getOriginalFilename(), e.getMessage());
                    rejectedFiles.add(file.getOriginalFilename() + " (Erro ao processar a duração)");
                }
            }
        }

        return processedFiles;
    }

    
    private String saveFile(MultipartFile file) throws IOException {
      
        String uploadDir = "/mnt/c/Users/micha/OneDrive/Documentos/video-editor-api/videos/"; 
        String filePath = uploadDir + file.getOriginalFilename();
    
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    
        File destinationFile = new File(filePath);
        file.transferTo(destinationFile);
        
        return filePath;  
    }

    private boolean isSupportedFormat(String fileFormat) {
        return fileFormat.equalsIgnoreCase("mp4")
                || fileFormat.equalsIgnoreCase("avi")
                || fileFormat.equalsIgnoreCase("mov");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            logger.error("Nome do arquivo inválido: {}", fileName);
            throw new IllegalArgumentException("O nome do arquivo é inválido ou não possui uma extensão válida.");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private boolean isValidVideoContent(MultipartFile file) {
        try {
            return file.getSize() > 0;
        } catch (Exception e) {
            logger.error("Erro ao validar conteúdo do vídeo: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Erro ao validar o conteúdo do vídeo.");
        }
    }

    public List<VideoFileDTO> getAllVideos() {
        return videoFileRepository.findAllVideos();
    }
}
