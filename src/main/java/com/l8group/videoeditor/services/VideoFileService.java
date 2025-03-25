package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dtos.VideoFileListDTO;
import com.l8group.videoeditor.dtos.VideoFileResponseDTO;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.NoVideosFoundException;
import com.l8group.videoeditor.metrics.VideoFileServiceMetrics;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.rabbit.producer.VideoProcessingProducer;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.utils.VideoDurationUtils;
import com.l8group.videoeditor.utils.VideoValidationUtils;

import io.micrometer.core.instrument.Timer;

@Service
public class VideoFileService {

    @Value("${video.upload.dir}")
    private String STORAGE_DIR;

    private final VideoFileRepository videoFileRepository;
    private final VideoProcessingProducer videoProcessingProducer;
    private final VideoFileServiceMetrics videoFileServiceMetrics;

    public VideoFileService(VideoFileRepository videoFileRepository, VideoProcessingProducer videoProcessingProducer,
            VideoFileServiceMetrics videoFileServiceMetrics) {
        this.videoFileRepository = videoFileRepository;
        this.videoProcessingProducer = videoProcessingProducer;
        this.videoFileServiceMetrics = videoFileServiceMetrics;
        System.out.println("🚀 VideoFileServiceMetrics injetado com sucesso!");

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VideoFileResponseDTO uploadVideo(MultipartFile file) throws IOException {
        videoFileServiceMetrics.incrementUploadRequests();
        Timer.Sample sample = videoFileServiceMetrics.startUploadTimer();

        try {
            validateFileFormat(file);
            videoFileServiceMetrics.setFileSize(file.getSize());

            // Obtém o nome original do arquivo
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isBlank()) {
                throw new IllegalArgumentException("O arquivo deve ter um nome válido.");
            }

            // Garantir que o nome do arquivo seja seguro para armazenamento
            String safeFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            String fileExtension = "";
            int lastDotIndex = safeFileName.lastIndexOf(".");
            if (lastDotIndex != -1) {
                fileExtension = safeFileName.substring(lastDotIndex + 1); // Pega só a extensão (ex: "mp4", "avi",
                                                                          // "mov")
            }

            // Caminho para armazenar o arquivo (mantém o nome original)
            String filePath = STORAGE_DIR + File.separator + safeFileName;
            // Criar diretório se não existir
            File directory = new File(STORAGE_DIR);
            if (!directory.exists() || !directory.isDirectory()) {
                directory.mkdirs();
            }

            // Salvar o arquivo fisicamente
            Path targetPath = Path.of(filePath);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            if (VideoValidationUtils.isVideoCorrupt(filePath)) {
                videoFileServiceMetrics.incrementFileValidationErrors();
                throw new IllegalArgumentException("O vídeo está corrompido e não pode ser processado.");
            }

            if (VideoValidationUtils.hasBlackFrames(filePath)) {
                videoFileServiceMetrics.incrementFileValidationErrors();
                throw new IllegalArgumentException("O vídeo contém quadros pretos indesejados.");
            }

            if (VideoValidationUtils.hasFrozenFrames(filePath)) {
                videoFileServiceMetrics.incrementFileValidationErrors();
                throw new IllegalArgumentException("O vídeo apresenta congelamentos.");
            }

            // Criar entidade e salvar no banco
            VideoFile videoFile = new VideoFile();
            videoFile.setVideoFileName(safeFileName);
            videoFile.setVideoFileSize(file.getSize());
            videoFile.setVideoFileFormat(fileExtension);
            videoFile.setCreatedTimes(ZonedDateTime.now());
            videoFile.setUpdatedTimes(ZonedDateTime.now());
            videoFile.setStatus(VideoStatusEnum.PROCESSING);
            videoFile.setVideoFilePath(filePath);

            // 🔹 Obtém a duração do vídeo antes de salvar
            String duration = VideoDurationUtils.getVideoDurationAsString(filePath);
            videoFile.setVideoDuration(duration);

            videoFile = videoFileRepository.save(videoFile);
            videoProcessingProducer.sendVideoId(videoFile.getId());

            videoFileServiceMetrics.incrementUploadSuccess();
            return new VideoFileResponseDTO(videoFile.getId(), videoFile.getVideoFileName(),
                    videoFile.getCreatedTimes());
        } catch (Exception e) {
            videoFileServiceMetrics.incrementUploadFailure(); // Registra falha no upload
            if (e instanceof IOException) {
                videoFileServiceMetrics.incrementFileStorageErrors(); // Registra erro de armazenamento
            }
            throw e;
        } finally {
            videoFileServiceMetrics.recordUploadDuration(sample); // Registra tempo total de upload
        }
    }

    public VideoFile getVideoById(UUID id) {
        return videoFileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vídeo não encontrado para o ID: " + id));
    }

    public List<VideoFileListDTO> listAllVideos() {
        List<VideoFileListDTO> videos = videoFileRepository.findAllVideos();

        if (videos.isEmpty()) {
            throw new NoVideosFoundException("Nenhum vídeo encontrado.");
        }

        return videos;
    }

    private void validateFileFormat(MultipartFile file) {
        if (file.isEmpty()) {
            videoFileServiceMetrics.incrementFileValidationErrors();
            throw new IllegalArgumentException("O arquivo de vídeo não pode estar vazio.");
        }

        String contentType = file.getContentType();
        if (!isValidVideoFormat(contentType)) {
            videoFileServiceMetrics.incrementFileValidationErrors();
            throw new IllegalArgumentException(
                    "Formato de arquivo não suportado. Apenas MP4, AVI e MOV são permitidos.");
        }
    }

    private boolean isValidVideoFormat(String contentType) {
        return contentType != null &&
                (contentType.equals("video/mp4") ||
                        contentType.equals("video/x-msvideo") ||
                        contentType.equals("video/quicktime"));
    }

}
