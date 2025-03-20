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
import com.l8group.videoeditor.models.VideoConversion;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.rabbit.producer.VideoConversionProducer;
import com.l8group.videoeditor.repositories.VideoConversionRepository;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoConversionRequest;
import com.l8group.videoeditor.utils.VideoProcessorUtils;
import com.l8group.videoeditor.utils.VideoUtils;

@Service
public class VideoConversionService {

    private static final Logger logger = LoggerFactory.getLogger(VideoConversionService.class);

    private final VideoFileRepository videoFileRepository;
    private final VideoConversionRepository videoConversionRepository;
    private final VideoConversionProducer videoConversionProducer;

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    public VideoConversionService(VideoFileRepository videoFileRepository, VideoConversionRepository videoConversionRepository,
                                  VideoConversionProducer videoConversionProducer) {
        this.videoFileRepository = videoFileRepository;
        this.videoConversionRepository = videoConversionRepository;
        this.videoConversionProducer = videoConversionProducer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) 
    public String convertVideo(VideoConversionRequest request, String previousFilePath) {
        logger.info("Iniciando conversão de vídeo. videoId={}, outputFormat={}, previousFilePath={}",
                request.getVideoId(), request.getOutputFormat(), previousFilePath);

        UUID videoId = UUID.fromString(request.getVideoId());
        String outputFormat = request.getOutputFormat();

        // Busca o vídeo no banco
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

        // Geração do nome do arquivo convertido
        String baseFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.')); // Nome sem extensão
        String convertedFileName = baseFileName + "_" + shortUUID + formattedDate + "_convert." + outputFormat;

        String outputFilePath = Paths.get(TEMP_DIR, convertedFileName).toString();
        logger.info("Arquivo de saída definido como: {}", outputFilePath);

        // Garante que o diretório temporário existe
        createTempDirectory();

        // Inicia o processo de conversão
        logger.info("Iniciando conversão do vídeo: {} → {} (Formato: {})", inputFilePath, outputFilePath, outputFormat);
        boolean success = VideoProcessorUtils.convertVideo(inputFilePath, outputFilePath, outputFormat);

        if (!success) {
            logger.error("Falha ao converter o vídeo para o formato {}", outputFormat);
            throw new RuntimeException("Falha ao converter o vídeo para o formato " + outputFormat);
        }
        logger.info("Conversão concluída com sucesso.");

        // Salva as informações da conversão no banco de dados
        VideoConversion videoConversion = createAndSaveVideoConversion(videoFile, outputFormat);
        logger.info("Registro de conversão salvo no banco de dados. ID={}", videoConversion.getId());

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

    private VideoConversion createAndSaveVideoConversion(VideoFile videoFile, String outputFormat) {
        VideoConversion videoConversion = new VideoConversion();
        videoConversion.setVideoFile(videoFile);
        videoConversion.setVideoTargetFormat(outputFormat);
        videoConversion.setVideoFileFormat(videoFile.getVideoFileFormat());
        videoConversion.setCreatedTimes(ZonedDateTime.now());
        videoConversion.setUpdatedTimes(ZonedDateTime.now());
        videoConversion.setStatus(VideoStatusEnum.PROCESSING);

        // Salva primeiro para obter o ID
        videoConversion = videoConversionRepository.save(videoConversion);

        // Envia a mensagem para o RabbitMQ após o salvamento
        videoConversionProducer.sendVideoConversionMessage(videoConversion.getId().toString());

        return videoConversion;
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
