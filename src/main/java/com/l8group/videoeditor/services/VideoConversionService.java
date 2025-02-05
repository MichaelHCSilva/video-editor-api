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
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Transactional
    public VideoConversionsDTO convertVideo(VideoConversionRequest request) {
        logger.info("Iniciando conversão para o vídeo ID: {}", request.getVideoId());

        Optional<VideoConversion> existingConversion = videoConversionRepository
                .findByVideoFileId(UUID.fromString(request.getVideoId()));

        if (existingConversion.isPresent()) {
            logger.warn("Conversão já realizada para este vídeo: {}", request.getVideoId());
            throw new IllegalArgumentException("Conversão já realizada para este vídeo.");
        }

        VideoFile originalVideo = videoFileRepository.findById(UUID.fromString(request.getVideoId()))
                .orElseThrow(() -> {
                    logger.error("Vídeo não encontrado para ID: {}", request.getVideoId());
                    return new IllegalArgumentException("Vídeo não encontrado");
                });

        String outputDirectoryPath = createOutputDirectory(originalVideo.getFilePath());
        String outputFileName = generateFileName(originalVideo, request.getFormat(), request.getVideoId());
        String outputFilePath = outputDirectoryPath + File.separator + outputFileName;

        VideoConversion videoConversion = new VideoConversion();
        videoConversion.setVideoFile(originalVideo);
        videoConversion.setFileName(outputFileName);
        videoConversion.setFileFormat(originalVideo.getFileFormat());
        videoConversion.setTargetFormat(request.getFormat());
        videoConversion.setStatus(VideoStatus.PROCESSING);
        videoConversion.setUpdatedAt(ZonedDateTime.now());
        videoConversionRepository.save(videoConversion);

        logger.info("Conversão iniciada para o arquivo: {}", outputFileName);

        try {
            executeFFmpegConversion(originalVideo.getFilePath(), outputFilePath);
        } catch (IOException | InterruptedException e) {
            logger.error("Erro ao converter o vídeo ID: {} - {}", request.getVideoId(), e.getMessage());
            throw new VideoProcessingException("Erro ao converter o vídeo.", e);
        }

        videoConversion.setUpdatedAt(ZonedDateTime.now());
        videoConversionRepository.save(videoConversion);

        logger.info("Conversão concluída para o vídeo ID: {}, Arquivo: {}", request.getVideoId(), outputFileName);

        return new VideoConversionsDTO(videoConversion);
    }

    private String createOutputDirectory(String originalFilePath) {
        String baseDir = new File(originalFilePath).getParent();
        String outputDirectoryPath = baseDir + File.separator + CONVERSION_SUBDIRECTORY;

        try {
            Files.createDirectories(Paths.get(outputDirectoryPath));
            logger.info("Diretório de saída criado: {}", outputDirectoryPath);
        } catch (IOException e) {
            logger.error("Erro ao criar diretório de saída: {}", outputDirectoryPath);
            throw new VideoProcessingException("Erro ao criar diretório de saída.", e);
        }
        return outputDirectoryPath;
    }

    private String generateFileName(VideoFile originalVideo, String targetFormat, String videoId) {
        String baseName = originalVideo.getFileName().replaceAll("\\.[^.]+$", "");
        String timestamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String fileName = baseName + "_" + videoId.substring(0, 8) + "_" + timestamp + "_converted." + targetFormat;
        logger.info("Nome do arquivo gerado: {}", fileName);

        return fileName;
    }

    private void executeFFmpegConversion(String inputFilePath, String outputFilePath)
            throws IOException, InterruptedException {
        String[] command = {
                "ffmpeg", "-i", inputFilePath,
                "-c:v", "libx264", "-crf", "18", "-preset", "slow",
                "-c:a", "aac", "-b:a", "192k", "-strict", "experimental", "-pix_fmt", "yuv420p",
                outputFilePath
        };

        logger.info("Executando comando FFmpeg: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("FFmpeg: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("Erro ao executar a conversão do vídeo. Código de saída: {}", exitCode);
            throw new VideoProcessingException("Erro ao executar a conversão do vídeo.");
        }

        logger.info("Conversão concluída com sucesso. Arquivo de saída: {}", outputFilePath);
    }
}
