package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dtos.VideoFileResponseDTO;
import com.l8group.videoeditor.dtos.VideoUploadResponseDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoFileRequest;
import com.l8group.videoeditor.utils.VideoDurationUtils;

@Service
public class VideoFileService {

    private final VideoFileRepository videoFileRepository;
    private static final String UPLOAD_DIR = "videos";

    public VideoFileService(VideoFileRepository videoFileRepository) {
        this.videoFileRepository = videoFileRepository;

    }

    public VideoUploadResponseDTO uploadVideo(VideoFileRequest request) {
        MultipartFile file = request.getFile();
        validateFile(file);

        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String originalFileName = file.getOriginalFilename();
        String fileFormat = getFileExtension(originalFileName);

        if (fileFormat.isEmpty()) {
            throw new VideoProcessingException("Formato de arquivo inválido. O arquivo deve ter uma extensão válida.");
        }

        String filePath = UPLOAD_DIR + File.separator + originalFileName;
        saveFile(file, filePath);

        String formattedDuration = getFormattedVideoDuration(filePath);

        VideoFile videoFile = saveVideoMetadata(file, originalFileName, fileFormat, formattedDuration);
        videoFile.setFilePath(filePath);

        System.out.println("Vídeo salvo com formato: " + fileFormat);

        return new VideoUploadResponseDTO(videoFile.getId(), "Upload realizado com sucesso!");
    }

    public List<VideoFileResponseDTO> listAllVideos() {
        return videoFileRepository.findAllVideos();
    }

    public VideoFile getVideoById(UUID videoId) {
        return videoFileRepository.findById(videoId)
                .map(videoFile -> {
                    String fileFormat = videoFile.getFileFormat();
                    String fileNameWithFormat = videoFile.getFileName();

                    // Garante que o nome do arquivo tenha a extensão correta
                    if (!fileNameWithFormat.endsWith("." + fileFormat)) {
                        fileNameWithFormat = fileNameWithFormat.replaceAll("\\.[^.]+$", "") + "." + fileFormat;
                    }

                    String filePath = UPLOAD_DIR + File.separator + fileNameWithFormat;
                    videoFile.setFilePath(filePath);
                    return videoFile;
                })
                .orElseThrow(() -> new VideoProcessingException("Vídeo não encontrado no banco de dados."));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new VideoProcessingException("O arquivo não pode estar vazio.");
        }

        String fileFormat = getFileExtension(file.getOriginalFilename());
        if (!fileFormat.matches("mp4|avi|mov")) {
            throw new VideoProcessingException("Formato de arquivo não suportado.");
        }
    }

    private String getFileExtension(String fileName) {
        return fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()
                : "";
    }

    private void saveFile(MultipartFile file, String filePath) {
        try {
            File destinationFile = new File(filePath);
            Files.copy(file.getInputStream(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new VideoProcessingException("Erro ao salvar o arquivo: " + e.getMessage());
        }
    }

    private String getFormattedVideoDuration(String filePath) {
        try {
            Duration duration = VideoDurationUtils.getVideoDuration(filePath);
            return VideoDurationUtils.formatDuration(duration); // 🔹 Retorna no formato HH:mm:ss
        } catch (Exception e) {
            throw new VideoProcessingException("Erro ao obter a duração do vídeo: " + e.getMessage());
        }
    }

    public VideoFile updateVideoFile(VideoFile videoFile) {
        if (!videoFileRepository.existsById(videoFile.getId())) {
            throw new VideoProcessingException("Vídeo não encontrado para atualização.");
        }
        return videoFileRepository.save(videoFile);
    }

    private VideoFile saveVideoMetadata(MultipartFile file, String fileName, String fileFormat,
            String formattedDuration) {
        VideoFile videoFile = new VideoFile();
        videoFile.setFileName(fileName);
        videoFile.setFileSize(file.getSize());
        videoFile.setFileFormat(fileFormat);
        videoFile.setUploadedAt(ZonedDateTime.now());
        videoFile.setCreatedAt(ZonedDateTime.now());
        videoFile.setDuration(formattedDuration);
        videoFile.setStatus(VideoStatus.PROCESSING);
        return videoFileRepository.save(videoFile);
    }
}
