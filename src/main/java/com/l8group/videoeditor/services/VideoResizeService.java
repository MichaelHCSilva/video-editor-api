package com.l8group.videoeditor.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.dtos.VideoResizeResponseDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoResize;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.repositories.VideoResizeRepository;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.utils.VideoResolutions;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@Service
public class VideoResizeService {

    private static final Logger logger = LoggerFactory.getLogger(VideoResizeService.class);
    private final VideoFileRepository videoFileRepository;
    private final VideoResizeRepository videoResizeRepository;
    private static final String OUTPUT_SUBDIR = "resized-videos";

    public VideoResizeService(VideoFileRepository videoFileRepository, VideoResizeRepository videoResizeRepository) {
        this.videoFileRepository = videoFileRepository;
        this.videoResizeRepository = videoResizeRepository;
    }

    @Transactional
    public VideoResizeResponseDTO resizeVideo(@Valid VideoResizeRequest videoResizeRequest) throws Exception {
        logger.info("Iniciando redimensionamento: {}", videoResizeRequest);

        if (videoResizeRequest.getVideoId() == null || videoResizeRequest.getVideoId().isBlank()) {
            throw new IllegalArgumentException("O ID do vídeo é obrigatório.");
        }
        if (videoResizeRequest.getWidth() == 0 || videoResizeRequest.getHeight() == 0) {
            throw new IllegalArgumentException("A largura e altura são obrigatórias para o redimensionamento.");
        }

        VideoFile originalVideo = getVideoById(videoResizeRequest.getVideoId());
        validateResolution(videoResizeRequest.getWidth(), videoResizeRequest.getHeight());

        String resolution = videoResizeRequest.getWidth() + "x" + videoResizeRequest.getHeight();

        Optional<VideoResize> existingResize = videoResizeRepository.findByVideoFileAndResolution(originalVideo,
                resolution);
        if (existingResize.isPresent()) {
            logger.warn("O vídeo '{}' já foi redimensionado para {} anteriormente.", originalVideo.getFileName(),
                    resolution);
            throw new IllegalStateException(
                    "Já existe um vídeo redimensionado para a resolução " + resolution + " associado a este ID.");
        }

        String originalFilePath = convertToWSLPath(originalVideo.getFilePath());
        String outputDirectory = getOutputDirectory(originalFilePath);
        createDirectoryIfNotExists(outputDirectory);

        String resizedFileName = generateFileName(originalVideo, resolution);
        String resizedFilePath = outputDirectory + File.separator + resizedFileName;

        executeFFmpeg(originalFilePath, videoResizeRequest.getWidth(), videoResizeRequest.getHeight(), resizedFilePath);
        verifyFileExists(resizedFilePath);

        VideoResize videoResize = saveResizedVideo(originalVideo, resizedFileName, resolution);

        return new VideoResizeResponseDTO(videoResize.getFileName(), videoResize.getResolution(),
                videoResize.getUploadedAt());
    }

    private VideoFile getVideoById(String videoId) {
        try {
            UUID uuid = UUID.fromString(videoId);
            return videoFileRepository.findById(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("Nenhum vídeo encontrado para o ID fornecido."));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("O ID do vídeo fornecido é inválido.");
        }
    }

    private void validateResolution(int width, int height) {
        if (!VideoResolutions.isValidResolution(width, height)) {
            throw new IllegalArgumentException(
                    "A resolução fornecida é inválida. Resoluções suportadas: " + getSupportedResolutions());
        }
    }

    private String convertToWSLPath(String filePath) {
        return filePath.startsWith("C:\\") ? "/mnt/c/" + filePath.substring(3).replace("\\", "/") : filePath;
    }

    private String getOutputDirectory(String originalFilePath) {
        return new File(originalFilePath).getParent() + File.separator + OUTPUT_SUBDIR;
    }

    private void createDirectoryIfNotExists(String path) {
        File directory = new File(path);
        if (!directory.exists())
            directory.mkdirs();
    }

    private String generateFileName(VideoFile video, String resolution) {
        return video.getFileName().replace("." + video.getFileFormat(),
                "_resized_" + resolution + "." + video.getFileFormat());
    }

    private void executeFFmpeg(String input, int width, int height, String output)
            throws IOException, InterruptedException {
        String[] command = {
                "ffmpeg", "-i", input,
                "-vf", "scale=" + width + ":" + height,
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", "23",
                "-c:a", "aac",
                output
        };

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(logger::debug);
        }

        if (process.waitFor() != 0) {
            throw new IOException("Erro ao executar FFmpeg.");
        }
    }

    private void verifyFileExists(String path) throws IOException {
        if (!new File(path).exists()) {
            throw new IOException("Falha ao criar o arquivo de vídeo.");
        }
    }

    private VideoResize saveResizedVideo(VideoFile video, String fileName, String resolution) {
        VideoResize videoResize = new VideoResize();
        videoResize.setVideoFile(video);
        videoResize.setFileName(fileName);
        videoResize.setResolution(resolution);
        videoResize.setStatus(VideoStatus.PROCESSING);
        videoResize.setUploadedAt(ZonedDateTime.now());

        return videoResizeRepository.save(videoResize);
    }

    private String getSupportedResolutions() {
        return String.join(", ", VideoResolutions.getValidResolutions());
    }
}
