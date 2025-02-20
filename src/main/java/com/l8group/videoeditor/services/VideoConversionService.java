package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.l8group.videoeditor.dtos.VideoConversionsDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.models.VideoConversion;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoConversionRepository;
import com.l8group.videoeditor.requests.VideoConversionRequest;

@Service
public class VideoConversionService {

    private final VideoConversionRepository videoConversionRepository;
    private final VideoFileService videoFileService;

    public VideoConversionService(VideoFileService videoFileService,
            VideoConversionRepository videoConversionRepository) {
        this.videoFileService = videoFileService;
        this.videoConversionRepository = videoConversionRepository;
    }

    public VideoConversionsDTO convertVideo(VideoConversionRequest request) throws IOException, InterruptedException {
        VideoFile originalVideo = videoFileService.getVideoById(UUID.fromString(request.getVideoId()));


        String inputFilePath = originalVideo.getFilePath();
        String targetFormat = request.getFormat();

        if (targetFormat == null || targetFormat.isEmpty()) {
            throw new VideoProcessingException("Formato de conversão inválido.");
        }

        String newFilePath = inputFilePath.replaceFirst("\\.[^.]+$", "") + "." + targetFormat;

        String tempFilePath = inputFilePath + "_temp." + targetFormat;

        executeConversionCommand(inputFilePath, tempFilePath, targetFormat);

        verifyConvertedFileExistence(tempFilePath);

        replaceOriginalFile(inputFilePath, tempFilePath, newFilePath, originalVideo);

        VideoConversion videoConversion = saveVideoConversion(originalVideo, targetFormat);

        return new VideoConversionsDTO(originalVideo.getFileName(), newFilePath, targetFormat,
                videoConversion.getCreatedAt());
    }

    public void convert(String inputFilePath, String outputFilePath, String format)
            throws IOException, InterruptedException {
        if (inputFilePath == null || outputFilePath == null || format == null || format.isEmpty()) {
            throw new IllegalArgumentException("Parâmetros inválidos para conversão.");
        }

        String tempFilePath = outputFilePath + "_temp." + format;

        executeConversionCommand(inputFilePath, tempFilePath, format);

        verifyConvertedFileExistence(tempFilePath);

        Files.move(new File(tempFilePath).toPath(), new File(outputFilePath).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void executeConversionCommand(String inputFilePath, String outputFilePath, String format)
            throws IOException, InterruptedException {

        if (!outputFilePath.endsWith("." + format)) {
            outputFilePath += "." + format;
        }

        String[] command = {
                "ffmpeg", "-y",
                "-i", inputFilePath,
                "-c:v", "libx264", "-preset", "fast", "-crf", "23",
                "-c:a", "aac", "-strict", "experimental",
                outputFilePath
        };

        Process process = new ProcessBuilder(command).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Erro ao converter o vídeo.");
        }
    }

    private void replaceOriginalFile(String originalFilePath, String tempFilePath, String newFilePath, VideoFile originalVideo) throws IOException {
        File tempFile = new File(tempFilePath);
        if (!tempFile.exists() || tempFile.length() == 0) {
            throw new IOException("Erro ao substituir o arquivo original após a conversão.");
        }

        Files.deleteIfExists(new File(originalFilePath).toPath());

        Files.move(tempFile.toPath(), new File(newFilePath).toPath(), StandardCopyOption.REPLACE_EXISTING);

        originalVideo.setFilePath(newFilePath);
        
        videoFileService.updateVideoFile(originalVideo);
    }

    private void verifyConvertedFileExistence(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            throw new IOException("Falha ao criar o arquivo de vídeo convertido.");
        }
    }

    private VideoConversion saveVideoConversion(VideoFile originalVideo, String targetFormat) {
        VideoConversion videoConversion = new VideoConversion();
        videoConversion.setVideoFile(originalVideo);
        videoConversion.setFileName(originalVideo.getFileName());
        videoConversion.setFileFormat(originalVideo.getFileFormat());
        videoConversion.setTargetFormat(targetFormat);
        videoConversion.setCreatedAt(ZonedDateTime.now());
        videoConversion.setUpdatedAt(ZonedDateTime.now());
        videoConversion.setStatus(VideoStatus.PROCESSING);

        return videoConversionRepository.save(videoConversion);
    }
}
