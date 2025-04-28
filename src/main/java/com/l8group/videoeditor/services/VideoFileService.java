package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
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
import com.l8group.videoeditor.utils.VideoFileStorageUtils;
import com.l8group.videoeditor.utils.VideoDurationUtils;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.validation.VideoValidationExecutor;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoFileService {

    @Value("${video.upload.dir}")
    private String STORAGE_DIR;

    private final VideoFileRepository videoFileRepository;
    private final VideoProcessingProducer videoProcessingProducer;
    private final VideoFileServiceMetrics videoFileServiceMetrics;
    private final VideoFileFinderService videoFileFinderService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VideoFileResponseDTO uploadVideo(MultipartFile file) throws IOException {
        videoFileServiceMetrics.incrementUploadRequests();
        Timer.Sample sample = videoFileServiceMetrics.startUploadTimer();

        File tempFile = null;

        try {
            validateFileFormat(file);
            videoFileServiceMetrics.setFileSize(file.getSize());

            String originalFileName = file.getOriginalFilename();
            String newFileName = VideoFileNameGenerator.generateUniqueFileName(originalFileName);
            String finalFilePath = VideoFileStorageUtils.buildFilePath(STORAGE_DIR, newFileName);

            VideoFileStorageUtils.createDirectoryIfNotExists(STORAGE_DIR);

            tempFile = File.createTempFile("upload_", "_" + originalFileName);
            file.transferTo(tempFile);
            log.info("Arquivo salvo temporariamente em: {}", tempFile.getAbsolutePath());

            VideoValidationExecutor.validateWithFFmpeg(tempFile.getAbsolutePath());

            Path targetPath = Path.of(finalFilePath);
            VideoFileStorageUtils.moveFile(tempFile.toPath(), targetPath);

            VideoFile videoFile = createVideoEntity(file, finalFilePath, newFileName);
            videoFile = videoFileRepository.save(videoFile);

            videoProcessingProducer.sendVideoId(videoFile.getId());

            videoFileServiceMetrics.incrementUploadSuccess();
            videoFileServiceMetrics.recordUploadDuration(sample);

            return new VideoFileResponseDTO(
                    videoFile.getId(),
                    videoFile.getVideoFileName(),
                    videoFile.getCreatedTimes());

        } catch (Exception e) {
            videoFileServiceMetrics.incrementFileValidationErrors();
            log.error("Erro durante upload de vídeo: {}", e.getMessage(), e);
            throw e;
        } finally {
            VideoFileStorageUtils.deleteFileIfExists(tempFile);
        }
    }

    public VideoFile getVideoById(UUID id) {
        return videoFileFinderService.findById(id); 
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
            throw new IllegalArgumentException("O arquivo de vídeo não pode estar vazio.");
        }
    }

    private VideoFile createVideoEntity(MultipartFile file, String filePath, String newFileName) throws IOException {
        String extension = newFileName.substring(newFileName.lastIndexOf(".") + 1);
        String duration = VideoDurationUtils.getVideoDurationAsString(filePath);

        VideoFile videoFile = new VideoFile();
        videoFile.setVideoFileName(newFileName);
        videoFile.setVideoFileSize(file.getSize());
        videoFile.setVideoFileFormat(extension);
        videoFile.setVideoDuration(duration);
        videoFile.setVideoFilePath(filePath);
        videoFile.setCreatedTimes(ZonedDateTime.now());
        videoFile.setUpdatedTimes(ZonedDateTime.now());
        videoFile.setStatus(VideoStatusEnum.PROCESSING);
        return videoFile;
    }
}
