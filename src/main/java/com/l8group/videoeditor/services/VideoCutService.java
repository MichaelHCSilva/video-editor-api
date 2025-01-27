package com.l8group.videoeditor.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.l8group.videoeditor.dtos.VideoCutResponseDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoCut;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoCutRepository;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.utils.VideoDuration;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@Service
public class VideoCutService {

    private final VideoFileRepository videoFileRepository;
    private final VideoCutRepository videoCutRepository;
    private final String outputSubdirectory = "videos-cuts";

    public VideoCutService(VideoFileRepository videoFileRepository, VideoCutRepository videoCutRepository) {
        this.videoFileRepository = videoFileRepository;
        this.videoCutRepository = videoCutRepository;
    }

    @Transactional
    public VideoCutResponseDTO cutVideo(@Valid VideoCutRequest videoCutRequest) throws Exception {
        Optional<VideoFile> optionalVideoFile = videoFileRepository
                .findById(UUID.fromString(videoCutRequest.getVideoId()));
        if (optionalVideoFile.isEmpty()) {
            throw new IllegalArgumentException("O vídeo com o ID fornecido não foi encontrado.");
        }
        VideoFile originalVideo = optionalVideoFile.get();

        String originalFilePath = convertToWSLPath(originalVideo.getFilePath());
        if (originalFilePath == null || originalFilePath.isBlank()) {
            throw new IllegalArgumentException("Caminho do vídeo original é inválido.");
        }

        Long originalDuration = getOriginalVideoDuration(originalFilePath);

        Long startTimeInSeconds = parseTimeToSeconds(videoCutRequest.getStartTime());
        Long endTimeInSeconds = parseTimeToSeconds(videoCutRequest.getEndTime());
        validateCutTimes(startTimeInSeconds, endTimeInSeconds, originalDuration);

        String outputDirectoryPath = getOutputDirectoryPath(originalFilePath);
        createDirectoryIfNotExists(outputDirectoryPath);
        String cutFileName = generateCutFileName(originalVideo);
        String cutFilePath = outputDirectoryPath + File.separator + cutFileName;

        executeCutCommand(originalFilePath, videoCutRequest.getStartTime(), videoCutRequest.getEndTime(), cutFilePath);

        verifyCutFileExistence(cutFilePath);

        Long cutDuration = VideoDuration.getVideoDurationInSeconds(cutFilePath);

        VideoCut videoCut = new VideoCut();
        videoCut.setVideoFile(originalVideo);
        videoCut.setFileName(cutFileName);
        videoCut.setDuration(cutDuration);
        videoCut.setUploadedAt(ZonedDateTime.now());
        videoCut.setStatus(VideoStatus.PROCESSING);

        videoCut = videoCutRepository.save(videoCut);

        // Retornando o DTO de resposta
        return new VideoCutResponseDTO(
                videoCut.getFileName(),
                formatDuration(cutDuration),
                videoCut.getUploadedAt());
    }

    private Long getOriginalVideoDuration(String originalFilePath) throws IOException {
        try {
            return VideoDuration.getVideoDurationInSeconds(originalFilePath);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Erro ao obter a duração do vídeo. Verifique o arquivo ou o FFmpeg.");
        }
    }

    private String convertToWSLPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        if (filePath.matches("^[A-Z]:\\\\.*")) {
            String driveLetter = filePath.substring(0, 1).toLowerCase();
            return "/mnt/" + driveLetter + "/" + filePath.substring(3).replace("\\", "/");
        }
        return filePath;
    }

    private void validateCutTimes(Long startTimeInSeconds, Long endTimeInSeconds, Long originalDuration) {
        final long minimumCutDuration = 1L;

        if (startTimeInSeconds.equals(endTimeInSeconds)) {
            throw new IllegalArgumentException(
                    String.format("O corte não é possível. O tempo de início (%s) e término (%s) são iguais. "
                            + "Defina um intervalo válido de pelo menos %d segundo(s).",
                            formatDuration(startTimeInSeconds), formatDuration(endTimeInSeconds), minimumCutDuration));
        }

        if ((endTimeInSeconds - startTimeInSeconds) < minimumCutDuration) {
            throw new IllegalArgumentException(
                    String.format("O corte é muito curto. A duração mínima permitida é de %d segundo(s). "
                            + "Por favor, ajuste os tempos de início (%s) e término (%s).",
                            minimumCutDuration, formatDuration(startTimeInSeconds), formatDuration(endTimeInSeconds)));
        }

        if (startTimeInSeconds >= endTimeInSeconds) {
            throw new IllegalArgumentException("O tempo de início deve ser menor que o tempo de término.");
        }

        if (endTimeInSeconds > originalDuration) {
            throw new IllegalArgumentException(
                    String.format("O tempo de término (%s) não pode ser maior que a duração do vídeo (%s).",
                            formatDuration(endTimeInSeconds), formatDuration(originalDuration)));
        }

        if (startTimeInSeconds < 0 || endTimeInSeconds < 0) {
            throw new IllegalArgumentException("Os tempos de início e término não podem ser negativos.");
        }
    }

    private String generateCutFileName(VideoFile originalVideo) {
        String originalFileName = originalVideo.getFileName();
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
        String originalFileFormat = originalVideo.getFileFormat();

        return baseName + "_cut." + originalFileFormat;
    }

    private void executeCutCommand(String originalFilePath, String startTime, String endTime, String cutFilePath)
            throws IOException, InterruptedException {
        String[] command = {
                "ffmpeg", "-i", originalFilePath,
                "-ss", startTime,
                "-to", endTime,
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", "23",
                "-c:a", "aac",
                "-strict", "experimental",
                cutFilePath
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Erro ao executar o comando FFmpeg. Código de saída: " + exitCode);
        }
    }

    private void verifyCutFileExistence(String cutFilePath) throws IOException {
        File cutFile = new File(cutFilePath);
        if (!cutFile.exists()) {
            throw new IOException("Falha ao criar o arquivo de vídeo cortado.");
        }
    }

    private void createDirectoryIfNotExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private String getOutputDirectoryPath(String originalFilePath) {
        File originalFile = new File(originalFilePath);
        return originalFile.getParent() + File.separator + outputSubdirectory;
    }

    private Long parseTimeToSeconds(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return (long) (hours * 3600 + minutes * 60 + seconds);
    }

    public static String formatDuration(Long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, sec);
    }
}
