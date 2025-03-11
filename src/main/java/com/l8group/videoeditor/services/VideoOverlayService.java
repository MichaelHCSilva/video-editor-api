package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoOverlay;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.repositories.VideoOverlayRepository;
import com.l8group.videoeditor.requests.VideoOverlayRequest;
import com.l8group.videoeditor.utils.VideoLuminosityProcessorUtils;
import com.l8group.videoeditor.utils.VideoUtils;

@Service
public class VideoOverlayService {

    private final VideoFileRepository videoFileRepository;
    private final VideoOverlayRepository videoOverlayRepository;
    private static final Logger logger = LoggerFactory.getLogger(VideoOverlayService.class);

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    public VideoOverlayService(VideoFileRepository videoFileRepository,
                               VideoOverlayRepository videoOverlayRepository) {
        this.videoFileRepository = videoFileRepository;
        this.videoOverlayRepository = videoOverlayRepository;
    }

    @Transactional
    public String processOverlay(VideoOverlayRequest request, String previousFilePath) {
        logger.info("Iniciando overlay no vídeo. videoId={}, watermark={}, position={}, fontSize={}, previousFilePath={}",
                request.getVideoId(), request.getWatermark(), request.getPosition(), request.getFontSize(), previousFilePath);

        UUID videoId = UUID.fromString(request.getVideoId());

        // Verifica se o vídeo existe no banco
        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> {
                    logger.error("Vídeo não encontrado para o ID: {}", videoId);
                    return new RuntimeException("Vídeo não encontrado para o ID: " + videoId);
                });

        logger.info("Vídeo encontrado: {} (caminho: {})", videoFile.getFileName(), videoFile.getFilePath());

        // Determina o arquivo de entrada
        String inputFilePath = (previousFilePath != null && !previousFilePath.isEmpty())
                ? previousFilePath
                : videoFile.getFilePath(); 

        logger.info("Arquivo de entrada definido como: {}", inputFilePath);

        // Verifica se o arquivo de entrada realmente existe
        File inputFile = new File(inputFilePath);
        if (!inputFile.exists()) {
            logger.error("Arquivo de entrada não encontrado: {}", inputFilePath);
            throw new RuntimeException("Arquivo de entrada não encontrado: " + inputFilePath);
        }

        String originalFileName = videoFile.getFileName();
        String shortUUID = VideoUtils.generateShortUUID();
        String formattedDate = VideoUtils.formatDate(LocalDate.now());

        // Nome do arquivo de saída
        String overlayFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                + "_" + shortUUID + formattedDate + "_overlay." + videoFile.getFileFormat().replace(".", "");

        String outputFilePath = Paths.get(TEMP_DIR, overlayFileName).toString();
        logger.info("Arquivo de saída definido como: {}", outputFilePath);

        // Garante que o diretório temporário existe
        createTempDirectory();

        // Processa o overlay
        logger.info("Iniciando processamento do overlay: {} → {}", inputFilePath, outputFilePath);
        boolean success = VideoLuminosityProcessorUtils.addTextOverlay(
                inputFilePath, outputFilePath, request.getWatermark(), request.getPosition(), request.getFontSize(), null);

        if (!success) {
            logger.error("Erro ao processar overlay no vídeo.");
            throw new RuntimeException("Erro ao processar overlay no vídeo.");
        }
        logger.info("Overlay aplicado com sucesso.");

        // Salva os dados do overlay no banco de dados
        VideoOverlay videoOverlay = createAndSaveVideoOverlay(videoFile, request);
        logger.info("Registro de overlay salvo no banco de dados. ID={}", videoOverlay.getId());

        return outputFilePath;
    }

    private void createTempDirectory() {
        Path tempDirPath = Paths.get(TEMP_DIR);
        if (!Files.exists(tempDirPath)) {
            try {
                Files.createDirectories(tempDirPath);
                logger.info("Diretório temporário criado: {}", TEMP_DIR);
            } catch (IOException e) {
                logger.error("Erro ao criar diretório temporário: {}", e.getMessage());
                throw new RuntimeException("Erro ao criar diretório temporário.", e);
            }
        }
    }

    private VideoOverlay createAndSaveVideoOverlay(VideoFile videoFile, VideoOverlayRequest request) {
        VideoOverlay videoOverlay = new VideoOverlay();
        videoOverlay.setVideoFile(videoFile);
        videoOverlay.setCreatedAt(ZonedDateTime.now());
        videoOverlay.setUpdatedAt(ZonedDateTime.now());
        videoOverlay.setStatus(VideoStatus.PROCESSING);
        videoOverlay.setWatermark(request.getWatermark());
        videoOverlay.setOverlayPosition(request.getPosition());
        videoOverlay.setFontSize(request.getFontSize());

        return videoOverlayRepository.save(videoOverlay);
    }

    // Remove arquivos temporários após a consolidação final
    public void deleteTemporaryFiles(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                logger.info("Arquivo temporário removido com sucesso: {}", filePath);
            } else {
                logger.error("Falha ao excluir arquivo temporário: {}", filePath);
            }
        } else {
            logger.warn("Tentativa de remover arquivo inexistente: {}", filePath);
        }
    }
}
