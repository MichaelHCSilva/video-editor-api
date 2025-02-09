package com.l8group.videoeditor.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
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

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<VideoResizeResponseDTO> resizeVideo(VideoResizeRequest videoResizeRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return processResize(videoResizeRequest);
            } catch (Exception ex) {
                logger.error("Erro ao redimensionar vídeo para resolução {}x{}: {}",
                        videoResizeRequest.getWidth(), videoResizeRequest.getHeight(), ex.getMessage(), ex);
                throw new RuntimeException("Erro no processamento do vídeo: " + ex.getMessage(), ex);
            }
        });
    }

    private VideoResizeResponseDTO processResize(@Valid VideoResizeRequest videoResizeRequest) throws Exception {
        logger.info("Iniciando redimensionamento: {}", videoResizeRequest);

        if (videoResizeRequest.getVideoId() == null || videoResizeRequest.getVideoId().isBlank()) {
            throw new IllegalArgumentException("O ID do vídeo é obrigatório.");
        }
        if (videoResizeRequest.getWidth() <= 0 || videoResizeRequest.getHeight() <= 0) {
            throw new IllegalArgumentException("A largura e altura devem ser maiores que zero.");
        }

        VideoFile originalVideo = getVideoById(videoResizeRequest.getVideoId());
        validateResolution(videoResizeRequest.getWidth(), videoResizeRequest.getHeight());

        String resolution = videoResizeRequest.getWidth() + "x" + videoResizeRequest.getHeight();

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
        if (video == null || resolution == null || resolution.trim().isEmpty()) {
            throw new IllegalArgumentException("O vídeo e a resolução não podem ser nulos ou vazios.");
        }

        String originalFileName = video.getFileName();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do arquivo original não pode ser nulo ou vazio.");
        }

        String baseName = originalFileName.replaceAll("\\.[^.]+$", ""); // Remove a extensão
        String timestamp = new SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String videoId = UUID.randomUUID().toString().substring(0, 8);

        return baseName + "_" + videoId + "_" + timestamp + "_" + resolution + "_resized." + video.getFileFormat();
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
        videoResize.setCreatedAt(ZonedDateTime.now());
        videoResize.setUploadedAt(ZonedDateTime.now());

        return videoResizeRepository.save(videoResize);
    }

    private String getSupportedResolutions() {
        return String.join(", ", VideoResolutions.getValidResolutions());
    }
}
