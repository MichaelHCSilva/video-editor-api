/*package com.l8group.videoeditor.services;

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

import com.l8group.videoeditor.dtos.VideoOverlayResponseDTO;
import com.l8group.videoeditor.enums.OverlayPosition;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoOverlay;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.repositories.VideoOverlayRepository;
import com.l8group.videoeditor.requests.VideoOverlayRequest;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class VideoOverlayService {

    private static final Logger logger = LoggerFactory.getLogger(VideoOverlayService.class);
    private final VideoOverlayRepository videoOverlayRepository;
    private final VideoFileRepository videoFileRepository;
    private final String outputSubdirectory = "processed-videos";

    public VideoOverlayService(VideoOverlayRepository videoOverlayRepository, VideoFileRepository videoFileRepository) {
        this.videoOverlayRepository = videoOverlayRepository;
        this.videoFileRepository = videoFileRepository;
    }

    @Transactional
    public VideoOverlayResponseDTO createOverlay(VideoOverlayRequest overlayRequest) {
        logger.info("Iniciando a aplicação de marca d'água no vídeo com ID: {}", overlayRequest.getVideoId());

        Optional<VideoFile> videoFileOptional = videoFileRepository
                .findById(UUID.fromString(overlayRequest.getVideoId()));
        if (videoFileOptional.isEmpty()) {
            logger.error("O vídeo com o ID {} não foi encontrado.", overlayRequest.getVideoId());
            throw new EntityNotFoundException("O vídeo com o ID fornecido não foi encontrado.");
        }

        VideoFile videoFile = videoFileOptional.get();
        logger.info("Vídeo encontrado: {}", videoFile.getFileName());

        VideoOverlay videoOverlay = new VideoOverlay();
        videoOverlay.setVideoFile(videoFile);
        videoOverlay.setStatus(VideoStatus.PROCESSING);
        videoOverlay.setOverlayData(overlayRequest.getOverlayData());
        videoOverlay.setOverlayPosition(overlayRequest.getPosition());
        videoOverlay.setFontSize(overlayRequest.getFontSize());

        String outputDirectoryPath = getOutputDirectoryPath(videoFile.getFilePath());
        createDirectoryIfNotExists(outputDirectoryPath);

        String overlayFileName = generateOverlayFileName(videoFile);
        String overlayFilePath = outputDirectoryPath + File.separator + overlayFileName;

        try {
            applyDynamicOverlayToVideo(videoFile.getFilePath(), overlayFilePath, overlayRequest);
        } catch (IOException | InterruptedException e) {
            logger.error("Erro ao aplicar marca d'água no vídeo: {}", e.getMessage());
            throw new RuntimeException("Erro ao aplicar marca d'água no vídeo.", e);
        }

        videoOverlay.setFilePath(overlayFilePath);
        videoOverlay.setCreatedAt(ZonedDateTime.now());
        videoOverlay = videoOverlayRepository.save(videoOverlay);

        logger.info("Marca d'água aplicada com sucesso no vídeo. ID: {}", videoOverlay.getId());

        return mapToResponseDTO(videoOverlay);
    }

    private String getOutputDirectoryPath(String originalFilePath) {
        File originalFile = new File(originalFilePath);
        return originalFile.getParent() + File.separator + outputSubdirectory;
    }

    private void createDirectoryIfNotExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private String generateOverlayFileName(VideoFile videoFile) {
        String originalFileFormat = videoFile.getFileFormat();
        return videoFile.getFileName().replace("." + originalFileFormat, "_with_overlay." + originalFileFormat);
    }

    private void applyDynamicOverlayToVideo(String originalFilePath, String overlayFilePath,
            VideoOverlayRequest overlayRequest) throws IOException, InterruptedException {

        int[] dimensions = getVideoDimensions(originalFilePath);
        int videoWidth = dimensions[0];
        int videoHeight = dimensions[1];

        validateOverlayPosition(overlayRequest.getPosition());
        String calculatedPosition = calculateOverlayPosition(
                overlayRequest.getPosition(), videoWidth, videoHeight, overlayRequest.getFontSize());

        // Defina o caminho da fonte de forma dinâmica
        String fontPath = "/caminho/para/sua/fonte.ttf"; // Esse valor pode ser extraído de configuração ou variáveis de
                                                         // ambiente.

        String brightnessFilter = String.format(
                "drawtext=text='%s':" +
                        "fontfile=%s:" +
                        "fontcolor=white:" +
                        "fontsize=%d:" +
                        "%s",
                overlayRequest.getOverlayData(), fontPath, overlayRequest.getFontSize(), calculatedPosition);

        String[] overlayCommand = {
                "ffmpeg", "-y", originalFilePath,
                "-vf", brightnessFilter,
                "-codec:v", "libx264", "-preset", "fast", "-crf", "18",
                "-codec:a", "aac", "-b:a", "192k", "-strict", "-2", overlayFilePath
        };

        ProcessBuilder overlayProcessBuilder = new ProcessBuilder(overlayCommand);
        overlayProcessBuilder.redirectErrorStream(true);
        Process overlayProcess = overlayProcessBuilder.start();

        // Captura a saída do processo FFmpeg para logar mensagens
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(overlayProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line); // Loga a saída do FFmpeg para diagnóstico.
            }
        }

        overlayProcess.waitFor();

        if (overlayProcess.exitValue() != 0) {
            logger.error("O comando FFmpeg falhou ao aplicar a marca d'água.");
            throw new RuntimeException("Erro ao aplicar a marca d'água no vídeo.");
        }
    }

    private int[] getVideoDimensions(String videoFilePath) throws IOException, InterruptedException {
        String[] dimensionCommand = {
                "ffprobe", "-v", "error", "-select_streams", "v:0",
                "-show_entries", "stream=width,height", "-of", "csv=p=0", videoFilePath
        };

        ProcessBuilder processBuilder = new ProcessBuilder(dimensionCommand);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        process.waitFor();

        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine();
            if (output != null && output.contains(",")) {
                String[] dimensions = output.split(",");
                int width = Integer.parseInt(dimensions[0]);
                int height = Integer.parseInt(dimensions[1]);
                return new int[] { width, height };
            }
        }

        throw new RuntimeException("Não foi possível obter as dimensões do vídeo.");
    }

    private String calculateOverlayPosition(OverlayPosition position, int videoWidth, int videoHeight, int fontSize) {
        return switch (position) {
            case CENTER -> {
                int xCenter = (videoWidth - (fontSize * 10)) / 2;
                int yCenter = (videoHeight - fontSize) / 2;
                yield String.format("x=%d:y=%d", xCenter, yCenter);
            }
            case TOP_LEFT -> "x=10:y=10";
            case TOP_RIGHT -> String.format("x=%d:y=10", videoWidth - (fontSize * 10) - 10);
            case BOTTOM_LEFT -> String.format("x=10:y=%d", videoHeight - fontSize - 10);
            case BOTTOM_RIGHT ->
                String.format("x=%d:y=%d", videoWidth - (fontSize * 10) - 10, videoHeight - fontSize - 10);
            default -> throw new IllegalArgumentException("A posição fornecida não é válida: " + position);
        };
    }

    private void validateOverlayPosition(OverlayPosition position) {
        if (position == null) {
            throw new IllegalArgumentException("A posição fornecida não é válida.");
        }
    }

    private VideoOverlayResponseDTO mapToResponseDTO(VideoOverlay videoOverlay) {
        VideoOverlayResponseDTO responseDTO = new VideoOverlayResponseDTO();
        responseDTO.setId(videoOverlay.getId());
        responseDTO.setOverlayData(videoOverlay.getOverlayData());
        responseDTO.setPosition(videoOverlay.getOverlayPosition());
        responseDTO.setFontSize(videoOverlay.getFontSize());
        return responseDTO;
    }

}
*/