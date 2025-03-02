package com.l8group.videoeditor.services;

import com.l8group.videoeditor.dtos.VideoCutResponseDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.models.VideoCut;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoCutRepository;
//import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.utils.VideoDurationUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class VideoCutService {

    private final VideoCutRepository videoCutRepository;
    private final VideoFileService videoFileService;

    public VideoCutService(VideoCutRepository videoCutRepository, VideoFileService videoFileService) {
        this.videoCutRepository = videoCutRepository;
        this.videoFileService = videoFileService;
    }

    public VideoCutResponseDTO cutVideo(VideoCutRequest request) throws IOException, InterruptedException {
        VideoFile originalVideo = videoFileService.getVideoById(UUID.fromString(request.getVideoId()));

        String inputFilePath = originalVideo.getFilePath();
        Duration originalDuration = VideoDurationUtils.getVideoDuration(inputFilePath);

        validateCutTimes(request.getStartTime(), request.getEndTime(), originalDuration);

        String tempFilePath = inputFilePath + "_temp.mp4";

        executeCutCommand(inputFilePath, request.getStartTime(), request.getEndTime(), tempFilePath);

        verifyCutFileExistence(tempFilePath);

        replaceOriginalFile(inputFilePath, tempFilePath);

        Duration cutDuration = request.getEndTime().minus(request.getStartTime());
        VideoCut videoCut = saveVideoCut(originalVideo, originalVideo.getFileName(), cutDuration);

        return new VideoCutResponseDTO(originalVideo.getFileName(), VideoDurationUtils.formatDuration(cutDuration),
                videoCut.getCreatedAt());
    }

    public String cut(String inputFilePath, String outputFilePath, Object parameters)
            throws IOException, InterruptedException {
        if (!(parameters instanceof VideoCutRequest)) {
            throw new IllegalArgumentException("Parâmetros inválidos para corte.");
        }

        VideoCutRequest request = (VideoCutRequest) parameters;
        Duration originalDuration = VideoDurationUtils.getVideoDuration(inputFilePath);

        validateCutTimes(request.getStartTime(), request.getEndTime(), originalDuration);

        executeCutCommand(inputFilePath, request.getStartTime(), request.getEndTime(), outputFilePath);

        verifyCutFileExistence(outputFilePath);

        return outputFilePath;
    }

    private void validateCutTimes(Duration startTime, Duration endTime, Duration originalDuration) {
        if (startTime.equals(endTime) || startTime.compareTo(endTime) >= 0 || endTime.compareTo(originalDuration) > 0) {
            throw new VideoProcessingException("Intervalo de tempo de corte inválido.");
        }
    }

    private void executeCutCommand(String inputFilePath, Duration startTime, Duration endTime, String tempFilePath)
            throws IOException, InterruptedException {
        String[] command = {
                "ffmpeg", "-y",
                "-i", inputFilePath,
                "-ss", formatDuration(startTime),
                "-to", formatDuration(endTime),
                "-c:v", "libx264", "-preset", "fast", "-crf", "23",
                "-c:a", "aac", "-strict", "experimental",
                tempFilePath
        };

        Process process = new ProcessBuilder(command).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Erro ao executar o FFmpeg.");
        }
    }

    private void verifyCutFileExistence(String tempFilePath) throws IOException {
        File tempFile = new File(tempFilePath);
        if (!tempFile.exists() || tempFile.length() == 0) {
            throw new IOException("Falha ao criar o arquivo de vídeo cortado.");
        }
    }

    private void replaceOriginalFile(String originalFilePath, String tempFilePath) throws IOException {
        File originalFile = new File(originalFilePath);
        File tempFile = new File(tempFilePath);

        if (!tempFile.exists() || tempFile.length() == 0) {
            throw new IOException("Erro ao substituir o arquivo original.");
        }

        Files.move(tempFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private VideoCut saveVideoCut(VideoFile originalVideo, String fileName, Duration cutDuration) {
        VideoCut videoCut = new VideoCut();
        videoCut.setVideoFile(originalVideo);
        videoCut.setFileName(fileName);
        videoCut.setDuration(formatDuration(cutDuration));
        videoCut.setCreatedAt(ZonedDateTime.now());
        videoCut.setUpdatedAt(ZonedDateTime.now());
        videoCut.setStatus(VideoStatus.PROCESSING);
        return videoCutRepository.save(videoCut);
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

}
