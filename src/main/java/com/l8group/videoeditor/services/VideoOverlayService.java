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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoOverlay;
import com.l8group.videoeditor.rabbit.producer.VideoOverlayProducer;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.repositories.VideoOverlayRepository;
import com.l8group.videoeditor.requests.VideoOverlayRequest;
import com.l8group.videoeditor.utils.VideoOverlayUtils;
import com.l8group.videoeditor.utils.VideoUtils;

@Service
public class VideoOverlayService {

    private static final Logger logger = LoggerFactory.getLogger(VideoOverlayService.class);

    private final VideoFileRepository videoFileRepository;
    private final VideoOverlayRepository videoOverlayRepository;
    private final VideoOverlayProducer videoOverlayProducer;

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    public VideoOverlayService(VideoFileRepository videoFileRepository,
            VideoOverlayRepository videoOverlayRepository, VideoOverlayProducer videoOverlayProducer) {
        this.videoFileRepository = videoFileRepository;
        this.videoOverlayRepository = videoOverlayRepository;
        this.videoOverlayProducer = videoOverlayProducer;

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String processOverlay(VideoOverlayRequest request, String previousFilePath) {
        logger.info(
                "Iniciando overlay no vídeo. videoId={}, watermark={}, position={}, fontSize={}, previousFilePath={}",
                request.getVideoId(), request.getWatermark(), request.getPosition(), request.getFontSize(),
                previousFilePath);

        UUID videoId = UUID.fromString(request.getVideoId());

        // Verifica se o vídeo existe no banco
        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> {
                    logger.error("Vídeo não encontrado para o ID: {}", videoId);
                    return new RuntimeException("Vídeo não encontrado para o ID: " + videoId);
                });

        logger.info("Vídeo encontrado: {} (caminho: {})", videoFile.getVideoFileName(), videoFile.getVideoFilePath());

        // Determina o arquivo de entrada
        String inputFilePath = (previousFilePath != null && !previousFilePath.isEmpty())
                ? previousFilePath
                : videoFile.getVideoFilePath();

        logger.info("Arquivo de entrada definido como: {}", inputFilePath);

        // Verifica se o arquivo de entrada realmente existe
        File inputFile = new File(inputFilePath);
        if (!inputFile.exists()) {
            logger.error("Arquivo de entrada não encontrado: {}", inputFilePath);
            throw new RuntimeException("Arquivo de entrada não encontrado: " + inputFilePath);
        }

        String originalFileName = videoFile.getVideoFileName();
        String shortUUID = VideoUtils.generateShortUuid();
        String formattedDate = VideoUtils.formatDateToCompactString(LocalDate.now());

        // Nome do arquivo de saída
        String overlayFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                + "_" + shortUUID + formattedDate + "_overlay." + videoFile.getVideoFileFormat().replace(".", "");

        String outputFilePath = Paths.get(TEMP_DIR, overlayFileName).toString();
        logger.info("Arquivo de saída definido como: {}", outputFilePath);

        // Garante que o diretório temporário existe
        createTempDirectory();

        // Processa o overlay
        logger.info("Iniciando processamento do overlay: {} → {}", inputFilePath, outputFilePath);
        boolean success = VideoOverlayUtils.applyTextOverlayWithFFmpeg(
                inputFilePath, outputFilePath, request.getWatermark(), request.getPosition(), request.getFontSize(),
                null);

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
        videoOverlay.setCreatedTimes(ZonedDateTime.now());
        videoOverlay.setUpdatedTimes(ZonedDateTime.now());
        videoOverlay.setStatus(VideoStatusEnum.PROCESSING);
        videoOverlay.setOverlayText(request.getWatermark());
        videoOverlay.setOverlayPosition(request.getPosition());
        videoOverlay.setOverlayFontSize(request.getFontSize());

        // Salva primeiro para obter o ID
        videoOverlay = videoOverlayRepository.save(videoOverlay);

        // Envia a mensagem para o RabbitMQ após o salvamento
        videoOverlayProducer.sendVideoOverlayMessage(videoOverlay.getId().toString());

        return videoOverlay;
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
