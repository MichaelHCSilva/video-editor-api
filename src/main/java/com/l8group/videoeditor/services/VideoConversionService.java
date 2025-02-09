package com.l8group.videoeditor.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.dtos.VideoConversionsDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.models.VideoConversion;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoConversionRepository;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoConversionRequest;

@Service
public class VideoConversionService {

    private static final Logger logger = LoggerFactory.getLogger(VideoConversionService.class);
    private static final String CONVERSION_SUBDIRECTORY = "videos-converted";

    private final VideoFileRepository videoFileRepository;
    private final VideoConversionRepository videoConversionRepository;

    public VideoConversionService(VideoFileRepository videoFileRepository,
            VideoConversionRepository videoConversionRepository) {
        this.videoFileRepository = videoFileRepository;
        this.videoConversionRepository = videoConversionRepository;
    }

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<VideoConversionsDTO> convertVideo(VideoConversionRequest request) {
        logger.info("Iniciando conversão para o vídeo ID: {}", request.getVideoId());

        VideoFile originalVideo = videoFileRepository.findById(UUID.fromString(request.getVideoId()))
                .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado"));

        String outputDirectoryPath = createOutputDirectory(originalVideo.getFilePath());
        String outputFileName = generateFileName(originalVideo, request.getFormat(), request.getVideoId());
        String outputFilePath = outputDirectoryPath + File.separator + outputFileName;

        VideoConversion videoConversion = new VideoConversion();
        videoConversion.setVideoFile(originalVideo);
        videoConversion.setFileName(outputFileName);
        videoConversion.setFileFormat(originalVideo.getFileFormat());
        videoConversion.setTargetFormat(request.getFormat());
        videoConversion.setStatus(VideoStatus.PROCESSING);
        videoConversion.setCreatedAt(ZonedDateTime.now());
        videoConversion.setUpdatedAt(ZonedDateTime.now());
        videoConversionRepository.save(videoConversion);

        try {
            executeFFmpegConversion(originalVideo.getFilePath(), outputFilePath);
        } catch (IOException | InterruptedException e) {
            logger.error("Erro ao converter vídeo ID: {} - {}", request.getVideoId(), e.getMessage());
        }

        videoConversion.setUpdatedAt(ZonedDateTime.now());
        videoConversionRepository.save(videoConversion);

        return CompletableFuture.completedFuture(new VideoConversionsDTO(videoConversion));
    }

    private String createOutputDirectory(String originalFilePath) {
        String baseDir = new File(originalFilePath).getParent();
        String outputDirectoryPath = baseDir + File.separator + CONVERSION_SUBDIRECTORY;
        try {
            Files.createDirectories(Paths.get(outputDirectoryPath));
        } catch (IOException e) {
            throw new VideoProcessingException("Erro ao criar diretório de saída.", e);
        }
        return outputDirectoryPath;
    }

    private String generateFileName(VideoFile video, String targetFormat, String videoId) {
        if (video == null || targetFormat == null || targetFormat.trim().isEmpty() || videoId == null
                || videoId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "O vídeo, formato de destino e ID do vídeo não podem ser nulos ou vazios.");
        }

        String originalFileName = video.getFileName();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do arquivo original não pode ser nulo ou vazio.");
        }

        String baseName = originalFileName.replaceAll("\\.[^.]+$", ""); 
        String timestamp = new SimpleDateFormat("yyyyMMdd").format(new Date());

        return baseName + "_" + videoId.substring(0, 8) + "_" + timestamp + "_converted." + targetFormat;
    }

    private void executeFFmpegConversion(String inputFilePath, String outputFilePath)
            throws IOException, InterruptedException {
        String[] command = {
                "ffmpeg", "-y", 
                "-i", inputFilePath,
                "-c:v", "libx264", "-crf", "23", "-preset", "fast",
                "-c:a", "aac", "-b:a", "128k", "-strict", "experimental", "-pix_fmt", "yuv420p",
                outputFilePath
        };

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("FFmpeg: {}", line);
            }
        }
        if (process.waitFor() != 0) {
            throw new VideoProcessingException("Erro na conversão do vídeo.");
        }
    }

}
