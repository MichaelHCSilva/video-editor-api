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
    private final VideoStatusManagerService videoStatusManagerService; // Injete o VideoStatusManagerService

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

        VideoOverlay overlayEntity = saveOverlayEntity(videoFile, request); 

        try {
            log.info("Aplicando overlay com texto '{}', posição '{}' e tamanho de fonte {}",
                    request.getWatermark(), request.getPosition(), request.getFontSize());

            boolean success = VideoOverlayUtils.applyTextOverlayWithFFmpeg(
                    inputFilePath, outputFilePath,
                    request.getWatermark(), request.getPosition(), request.getFontSize(), null
            );

            if (!success) {
                log.error("Falha na aplicação do overlay via FFmpeg.");
                videoStatusManagerService.updateEntityStatus(
                        videoOverlayRepository, overlayEntity.getId(), VideoStatusEnum.ERROR, "VideoOverlayService - FFmpeg Failure");
                throw new VideoProcessingException("Erro ao aplicar overlay.");
            }

            log.info("Overlay aplicado com sucesso para o vídeo: {}", videoId);
            videoStatusManagerService.updateEntityStatus(
                    videoOverlayRepository, overlayEntity.getId(), VideoStatusEnum.COMPLETED, "VideoOverlayService - Conclusão");

        } catch (VideoProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado durante o processamento de overlay para vídeo ID {}: {}", videoId, e.getMessage(), e);
            videoStatusManagerService.updateEntityStatus(
                    videoOverlayRepository, overlayEntity.getId(), VideoStatusEnum.ERROR, "VideoOverlayService - Unexpected Error");
            throw new VideoProcessingException("Erro inesperado ao aplicar overlay.", e);
        }

        log.info("Enviando mensagem para o RabbitMQ com ID do vídeo: {}", videoId);
        videoOverlayProducer.sendVideoOverlayMessage(videoId);

        log.info("Processo de overlay finalizado. Caminho de saída: {}", outputFilePath);
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

    private VideoOverlay saveOverlayEntity(VideoFile videoFile, VideoOverlayRequest request) {
        log.debug("Salvando entidade de overlay no banco para vídeo ID: {}", videoFile.getId());
        VideoOverlay overlay = new VideoOverlay();
        overlay.setVideoFile(videoFile);
        overlay.setOverlayText(request.getWatermark());
        overlay.setOverlayPosition(request.getPosition());
        overlay.setOverlayFontSize(request.getFontSize());
        overlay.setStatus(VideoStatusEnum.PROCESSING);
        overlay.setCreatedTimes(ZonedDateTime.now());
        overlay.setUpdatedTimes(ZonedDateTime.now());
        return videoOverlayRepository.save(overlay);
    }

    public void deleteTemporaryFiles(String filePath) {
        log.debug("Tentando deletar arquivo temporário: {}", filePath);
        VideoFileStorageUtils.deleteFileIfExists(new File(filePath));
        log.info("Arquivo temporário deletado (se existia): {}", filePath);
    }
}