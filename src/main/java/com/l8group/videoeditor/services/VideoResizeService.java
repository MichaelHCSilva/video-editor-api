package com.l8group.videoeditor.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.dtos.VideoResizeResponseDTO;
import com.l8group.videoeditor.enums.VideoResolution;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoResize;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.repositories.VideoResizeRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@Service
public class VideoResizeService {

    private static final Logger logger = LoggerFactory.getLogger(VideoResizeService.class);

    private final VideoFileRepository videoFileRepository;
    private final VideoResizeRepository videoResizeRepository;
    private final String outputSubdirectory = "resized-videos";

    public VideoResizeService(VideoFileRepository videoFileRepository, VideoResizeRepository videoResizeRepository) {
        this.videoFileRepository = videoFileRepository;
        this.videoResizeRepository = videoResizeRepository;
    }

    @Transactional
    public VideoResize resizeVideo(@Valid VideoResizeResponseDTO videoResizeDTO) throws Exception {
        logger.info("Iniciando o processo de redimensionamento de vídeo: {}", videoResizeDTO);

        if (videoResizeDTO.getVideoId() == null || videoResizeDTO.getVideoId().isBlank()) {
            logger.error("O ID do vídeo é obrigatório.");
            throw new IllegalArgumentException("O ID do vídeo é obrigatório.");
        }

        logger.debug("Buscando vídeo com ID: {}", videoResizeDTO.getVideoId());
        Optional<VideoFile> optionalVideoFile = videoFileRepository
                .findById(UUID.fromString(videoResizeDTO.getVideoId()));
        if (optionalVideoFile.isEmpty()) {
            logger.error("Vídeo com ID {} não encontrado.", videoResizeDTO.getVideoId());
            throw new IllegalArgumentException("O vídeo com o ID fornecido não foi encontrado.");
        }
        VideoFile originalVideo = optionalVideoFile.get();
        logger.info("Vídeo encontrado: {}", originalVideo);

        if (videoResizeDTO.getWidth() <= 0 || videoResizeDTO.getHeight() <= 0) {
            logger.error("Resolução inválida: {}x{}", videoResizeDTO.getWidth(), videoResizeDTO.getHeight());
            throw new IllegalArgumentException("A largura e altura devem ser maiores que zero.");
        }

        if (!isResolutionSupported(videoResizeDTO.getWidth(), videoResizeDTO.getHeight())) {
            String supportedResolutions = getSupportedResolutions();
            logger.error("Resolução não suportada: {}x{}. Resoluções suportadas: {}",
                    videoResizeDTO.getWidth(), videoResizeDTO.getHeight(), supportedResolutions);
            throw new IllegalArgumentException(
                    "Resolução não suportada: " + videoResizeDTO.getWidth() + "x" + videoResizeDTO.getHeight()
                            + ". Resoluções suportadas: " + supportedResolutions);
        }

        // Verificar se já existe um vídeo redimensionado com as mesmas dimensões
        Optional<VideoResize> existingResize = videoResizeRepository.findByVideoFileAndResolution(
                originalVideo,
                VideoResolution.fromDimensions(videoResizeDTO.getWidth(), videoResizeDTO.getHeight()));
        if (existingResize.isPresent()) {
            logger.warn("Vídeo com essa resolução já existe: {}x{}", videoResizeDTO.getWidth(),
                    videoResizeDTO.getHeight());
            throw new IllegalArgumentException("Vídeo com essa resolução já existe.");
        }

        String originalFilePath = convertToWSLPath(originalVideo.getFilePath());
        if (originalFilePath == null || originalFilePath.isBlank()) {
            logger.error("Caminho do vídeo original inválido.");
            throw new IllegalArgumentException("Caminho do vídeo original é inválido.");
        }
        logger.debug("Caminho do vídeo original (convertido): {}", originalFilePath);

        String outputDirectoryPath = getOutputDirectoryPath(originalFilePath);
        logger.debug("Criando diretório de saída: {}", outputDirectoryPath);
        createDirectoryIfNotExists(outputDirectoryPath);

        String resizedFileName = generateResizedFileName(originalVideo, videoResizeDTO.getWidth(),
                videoResizeDTO.getHeight());
        String resizedFilePath = outputDirectoryPath + File.separator + resizedFileName;
        logger.info("Caminho do vídeo redimensionado: {}", resizedFilePath);

        executeResizeCommand(originalFilePath, videoResizeDTO.getWidth(), videoResizeDTO.getHeight(), resizedFilePath);

        verifyResizedFileExistence(resizedFilePath);

        VideoResize videoResize = new VideoResize();
        videoResize.setVideoFile(originalVideo);
        videoResize.setFileName(resizedFileName);
        videoResize.setStatus(VideoStatus.PROCESSING);
        videoResize.setResolution(VideoResolution.fromDimensions(videoResizeDTO.getWidth(), videoResizeDTO.getHeight()));
        videoResize.setUploadedAt(java.time.ZonedDateTime.now());

        logger.info("Salvando metadados do vídeo redimensionado no banco de dados.");
        return videoResizeRepository.save(videoResize);
    }

    private String convertToWSLPath(String filePath) {
        String convertedPath = filePath;
        if (filePath != null && filePath.startsWith("C:\\")) {
            convertedPath = "/mnt/c/" + filePath.substring(3).replace("\\", "/");
        }
        logger.debug("Convertendo caminho para WSL: {} -> {}", filePath, convertedPath);
        return convertedPath;
    }

    private String getOutputDirectoryPath(String originalFilePath) {
        File originalFile = new File(originalFilePath);
        return originalFile.getParent() + File.separator + outputSubdirectory;
    }

    private void createDirectoryIfNotExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            logger.debug("Criando diretório: {}", directoryPath);
            directory.mkdirs();
        }
    }

    private String generateResizedFileName(VideoFile originalVideo, int width, int height) {
        String originalFileFormat = originalVideo.getFileFormat();
        // Modificando a geração do nome para incluir resolução e formato
        String resizedFileName = originalVideo.getFileName().replace("." + originalFileFormat,
                "_resized_" + width + "x" + height + "." + originalFileFormat);

        logger.debug("Gerando nome do arquivo redimensionado: {}", resizedFileName);
        return resizedFileName;
    }

    private void executeResizeCommand(String originalFilePath, int width, int height, String resizedFilePath)
            throws IOException, InterruptedException {
        String[] command = {
                "ffmpeg", "-i", originalFilePath,
                "-vf", "scale=" + width + ":" + height,
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", "23",
                "-c:a", "aac",
                "-strict", "experimental",
                resizedFilePath
        };

        logger.info("Executando comando FFmpeg: {}", String.join(" ", command));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("Erro ao executar FFmpeg. Código de saída: {}", exitCode);
            throw new IOException("Erro ao executar o comando FFmpeg. Código de saída: " + exitCode);
        }
    }

    private void verifyResizedFileExistence(String resizedFilePath) throws IOException {
        File resizedFile = new File(resizedFilePath);
        if (!resizedFile.exists()) {
            logger.error("Falha ao criar o arquivo de vídeo redimensionado: {}", resizedFilePath);
            throw new IOException("Falha ao criar o arquivo de vídeo redimensionado.");
        }
        logger.info("Arquivo de vídeo redimensionado criado com sucesso: {}", resizedFilePath);
    }

    private boolean isResolutionSupported(int width, int height) {
        try {
            VideoResolution.fromDimensions(width, height);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String getSupportedResolutions() {
        return java.util.Arrays.stream(VideoResolution.values())
                .map(res -> res.getWidth() + "x" + res.getHeight())
                .collect(Collectors.joining(", "));
    }
}
