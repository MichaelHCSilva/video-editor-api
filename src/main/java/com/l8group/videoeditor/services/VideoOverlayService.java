package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.metrics.VideoOverlayServiceMetrics;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoOverlay;
import com.l8group.videoeditor.rabbit.producer.VideoOverlayProducer;
import com.l8group.videoeditor.repositories.VideoOverlayRepository;
import com.l8group.videoeditor.requests.VideoOverlayRequest;
import com.l8group.videoeditor.utils.VideoFileStorageUtils;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.utils.VideoOverlayUtils;
import com.l8group.videoeditor.validation.VideoOverlayValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.ZonedDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class VideoOverlayService {

    private final VideoOverlayRepository videoOverlayRepository;
    private final VideoOverlayProducer videoOverlayProducer;
    private final VideoOverlayServiceMetrics videoOverlayServiceMetrics;
    private final VideoFileFinderService videoFileFinderService;
    private final VideoOverlayValidator videoOverlayValidator;
    

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    @Transactional
    public String processOverlay(VideoOverlayRequest request, String previousFilePath) {
        log.info("Iniciando processo de overlay para vídeo ID: {}", request.getVideoId());
        videoOverlayServiceMetrics.incrementOverlayRequests();

        // Valida a requisição
        log.debug("Validando requisição de overlay: {}", request);
        videoOverlayValidator.validate(request);

        String videoId = request.getVideoId();
        log.debug("Buscando vídeo no banco com ID: {}", videoId);
        VideoFile videoFile = videoFileFinderService.findById(videoId);

        String inputFilePath = previousFilePath != null ? previousFilePath : videoFile.getVideoFilePath();
        log.debug("Caminho do arquivo de entrada determinado: {}", inputFilePath);

        validateInputFile(inputFilePath);

        String outputFilePath = prepareOutputFile(videoFile.getVideoFileName());
        log.info("Arquivo de saída será gerado em: {}", outputFilePath);

        try {
            log.info("Aplicando overlay com texto '{}', posição '{}' e tamanho de fonte {}",
                    request.getWatermark(), request.getPosition(), request.getFontSize());

            boolean success = VideoOverlayUtils.applyTextOverlayWithFFmpeg(
                    inputFilePath, outputFilePath,
                    request.getWatermark(), request.getPosition(), request.getFontSize(), null
            );

            if (!success) {
                log.error("Falha na aplicação do overlay via FFmpeg.");
                throw new VideoProcessingException("Erro ao aplicar overlay.");
            }

            log.info("Overlay aplicado com sucesso para o vídeo: {}", videoId);

        } catch (Exception e) {
            log.error("Erro inesperado durante o processamento de overlay para vídeo ID {}: {}", videoId, e.getMessage(), e);
            throw new VideoProcessingException("Erro inesperado ao aplicar overlay.", e);
        }

        saveOverlayEntity(videoFile, request);

        log.info("Enviando mensagem para o RabbitMQ com ID do vídeo: {}", videoId);
        videoOverlayProducer.sendVideoOverlayMessage(videoId);

        log.info("Processo de overlay finalizado com sucesso. Caminho de saída: {}", outputFilePath);
        return outputFilePath;
    }

    private void validateInputFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.error("Arquivo de entrada não encontrado: {}", filePath);
            throw new VideoProcessingException("Vídeo inexistente ou removido para o ID especificado.");
        }
        log.debug("Arquivo de entrada validado com sucesso: {}", filePath);
    }

    private String prepareOutputFile(String originalFileName) {
        log.debug("Criando diretório temporário, se necessário: {}", TEMP_DIR);
        VideoFileStorageUtils.createDirectoryIfNotExists(TEMP_DIR);

        String generatedFilePath = VideoFileStorageUtils.buildFilePath(
                TEMP_DIR, VideoFileNameGenerator.generateFileNameWithSuffix(originalFileName, "overlay")
        );
        log.debug("Caminho do arquivo de saída preparado: {}", generatedFilePath);
        return generatedFilePath;
    }

    private void saveOverlayEntity(VideoFile videoFile, VideoOverlayRequest request) {
        log.debug("Salvando entidade de overlay no banco para vídeo ID: {}", videoFile.getId());
        VideoOverlay overlay = new VideoOverlay();
        overlay.setVideoFile(videoFile);
        overlay.setOverlayText(request.getWatermark());
        overlay.setOverlayPosition(request.getPosition());
        overlay.setOverlayFontSize(request.getFontSize());
        overlay.setStatus(VideoStatusEnum.PROCESSING);
        overlay.setCreatedTimes(ZonedDateTime.now());
        overlay.setUpdatedTimes(ZonedDateTime.now());
        videoOverlayRepository.save(overlay);
        log.info("Entidade de overlay salva com status PROCESSING para vídeo ID: {}", videoFile.getId());
    }

    public void deleteTemporaryFiles(String filePath) {
        log.debug("Tentando deletar arquivo temporário: {}", filePath);
        VideoFileStorageUtils.deleteFileIfExists(new File(filePath));
        log.info("Arquivo temporário deletado (se existia): {}", filePath);
    }
}
