package com.l8group.videoeditor.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.l8group.videoeditor.dtos.VideoCutResponseDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.models.VideoCut;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoCutRepository;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.utils.VideoDurationUtils;

import jakarta.transaction.Transactional;

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
    public VideoCutResponseDTO cutVideo(VideoCutRequest videoCutRequest) throws IOException, InterruptedException {
        VideoFile originalVideo = getOriginalVideo(videoCutRequest.getVideoId());
        String originalFilePath = convertToWSLPath(originalVideo.getFilePath());
        Duration originalDuration = VideoDurationUtils.getVideoDuration(originalFilePath);

        validateCutTimes(videoCutRequest.getStartTime(), videoCutRequest.getEndTime(), originalDuration);

        String outputDirectoryPath = createOutputDirectory(originalFilePath);
        String fileName = generateFileName(originalVideo);
        String cutFilePath = outputDirectoryPath + File.separator + fileName;

        executeCutCommand(originalFilePath, videoCutRequest.getStartTime(), videoCutRequest.getEndTime(), cutFilePath);
        verifyCutFileExistence(cutFilePath);

        Duration cutDuration = VideoDurationUtils.getVideoDuration(cutFilePath);
        if (cutDuration.isZero() || cutDuration.toSeconds() < 1) {
            throw new VideoProcessingException("O arquivo cortado possui duração inválida. O registro foi cancelado.");
        }

        VideoCut videoCut = saveVideoCut(originalVideo, fileName, cutDuration);

        return new VideoCutResponseDTO(
                videoCut.getFileName(),
                VideoDurationUtils.formatDuration(cutDuration),
                videoCut.getUploadedAt());
    }

    private VideoFile getOriginalVideo(String videoId) {
        Optional<VideoFile> optionalVideoFile = videoFileRepository
                .findById(UUID.fromString(videoId));
        if (optionalVideoFile.isEmpty()) {
            throw new VideoProcessingException("O vídeo com o ID fornecido não foi encontrado.");
        }
        return optionalVideoFile.get();
    }

    private String convertToWSLPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new VideoProcessingException("Caminho do vídeo original é inválido.");
        }
        if (filePath.matches("^[A-Z]:\\\\.*")) {
            String driveLetter = filePath.substring(0, 1).toLowerCase();
            return "/mnt/" + driveLetter + "/" + filePath.substring(3).replace("\\", "/");
        }
        return filePath;
    }

    private void validateCutTimes(Duration startTime, Duration endTime, Duration originalDuration) {
        if (startTime.equals(endTime)) {
            throw new VideoProcessingException(
                "O corte não é permitido porque o tempo de início e o tempo de término são iguais, resultando em uma duração nula para o corte.");
        }
    
        if (endTime.minus(startTime).equals(originalDuration)) {
            throw new VideoProcessingException(
                "O corte não pode ser igual à duração total do vídeo. Por favor, ajuste os tempos de início e término para um intervalo menor que a duração total do vídeo.");
        }
    
        if (endTime.minus(startTime).toSeconds() < 1) {
            throw new VideoProcessingException(
                "O tempo de início precisa ser anterior ao tempo de término. Verifique os valores e tente novamente.");
        }
    
        if (startTime.compareTo(endTime) >= 0) {
            throw new VideoProcessingException(
                "O tempo de início deve ser menor que o tempo de término. Corrija os valores e tente novamente.");
        }
    
        if (endTime.compareTo(originalDuration) > 0) {
            throw new VideoProcessingException(
                String.format("O tempo de término (%s) não pode ser maior que a duração total do vídeo (%s).",
                    VideoDurationUtils.formatDuration(endTime),
                    VideoDurationUtils.formatDuration(originalDuration)));
        }
    }
    

    private String createOutputDirectory(String originalFilePath) {
        File originalFile = new File(originalFilePath);
        String outputDirectoryPath = originalFile.getParent() + File.separator + outputSubdirectory;
        File directory = new File(outputDirectoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return outputDirectoryPath;
    }

    private String generateFileName(VideoFile originalVideo) {
        String originalFileName = originalVideo.getFileName();
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
        String originalFileFormat = originalVideo.getFileFormat();
        return baseName + "_cut." + originalFileFormat;
    }

    private void executeCutCommand(String originalFilePath, Duration startTime, Duration endTime, String cutFilePath)
            throws IOException, InterruptedException {
        String[] command = {
                "ffmpeg", "-i", originalFilePath,
                "-ss", VideoDurationUtils.formatDuration(startTime),
                "-to", VideoDurationUtils.formatDuration(endTime),
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
            throw new IOException("Erro ao executar o comando FFmpeg.");
        }

        // Valida a duração do arquivo gerado
        Duration cutDuration = VideoDurationUtils.getVideoDuration(cutFilePath);
        if (cutDuration.isZero() || cutDuration.toSeconds() < 1) {
            throw new VideoProcessingException("O corte gerado possui duração inválida. Verifique os tempos informados.");
        }
    }

    private void verifyCutFileExistence(String cutFilePath) throws IOException {
        File cutFile = new File(cutFilePath);
        if (!cutFile.exists() || cutFile.length() == 0) {
            throw new IOException("Falha ao criar o arquivo de vídeo cortado. Caminho: " + cutFilePath);
        }
    }

    private VideoCut saveVideoCut(VideoFile originalVideo, String fileName, Duration cutDuration) {
        VideoCut videoCut = new VideoCut();
        videoCut.setVideoFile(originalVideo);
        videoCut.setFileName(fileName);
        videoCut.setDuration(cutDuration.getSeconds());
        videoCut.setUploadedAt(ZonedDateTime.now());
        videoCut.setStatus(VideoStatus.PROCESSING);
        return videoCutRepository.save(videoCut);
    }
}
