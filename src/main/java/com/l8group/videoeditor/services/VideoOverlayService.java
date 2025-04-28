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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoOverlayService {

    private final VideoOverlayRepository videoOverlayRepository;
    private final VideoOverlayProducer videoOverlayProducer;
    private final VideoOverlayServiceMetrics videoOverlayServiceMetrics;
    private final VideoFileFinderService videoFileFinderService; // Usando o serviço de busca
    private final VideoOverlayValidator videoOverlayValidator;

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    @Transactional
    public String processOverlay(VideoOverlayRequest request, String previousFilePath) {
        videoOverlayServiceMetrics.incrementOverlayRequests();

        // Valida a requisição
        videoOverlayValidator.validate(request);

        // Obtém o ID do vídeo
        UUID videoId = UUID.fromString(request.getVideoId());

        // Verifica se o vídeo existe no banco; caso contrário, lança uma exceção
        VideoFile videoFile = videoFileFinderService.findById(videoId);

        // Determina o caminho do arquivo de entrada
        String inputFilePath = previousFilePath != null ? previousFilePath : videoFile.getVideoFilePath();

        // Valida se o arquivo de entrada existe
        validateInputFile(inputFilePath);

        // Prepara o caminho do arquivo de saída
        String outputFilePath = prepareOutputFile(videoFile.getVideoFileName());

        try {
            // Aplica o overlay no vídeo utilizando o FFmpeg
            boolean success = VideoOverlayUtils.applyTextOverlayWithFFmpeg(
                    inputFilePath, outputFilePath,
                    request.getWatermark(), request.getPosition(), request.getFontSize(), null
            );

            if (!success) {
                throw new VideoProcessingException("Erro ao aplicar overlay.");
            }
        } catch (Exception e) {
            throw new VideoProcessingException("Erro inesperado ao aplicar overlay.", e);
        }

        // Salva a entidade de overlay no banco de dados
        saveOverlayEntity(videoFile, request);

        // Envia a mensagem para o RabbitMQ
        videoOverlayProducer.sendVideoOverlayMessage(videoId.toString());

        // Retorna o caminho do arquivo de saída
        return outputFilePath;
    }

    private void validateInputFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.error("Arquivo de entrada não encontrado: {}", filePath);
            throw new VideoProcessingException("Vídeo inexistente ou removido para o ID especificado.");
        }
    }

    private String prepareOutputFile(String originalFileName) {
        VideoFileStorageUtils.createDirectoryIfNotExists(TEMP_DIR);
        return VideoFileStorageUtils.buildFilePath(
                TEMP_DIR, VideoFileNameGenerator.generateFileNameWithSuffix(originalFileName, "overlay")
        );
    }

    private void saveOverlayEntity(VideoFile videoFile, VideoOverlayRequest request) {
        VideoOverlay overlay = new VideoOverlay();
        overlay.setVideoFile(videoFile);
        overlay.setOverlayText(request.getWatermark());
        overlay.setOverlayPosition(request.getPosition());
        overlay.setOverlayFontSize(request.getFontSize());
        overlay.setStatus(VideoStatusEnum.PROCESSING);
        overlay.setCreatedTimes(ZonedDateTime.now());
        overlay.setUpdatedTimes(ZonedDateTime.now());
        videoOverlayRepository.save(overlay);
    }

    public void deleteTemporaryFiles(String filePath) {
        VideoFileStorageUtils.deleteFileIfExists(new File(filePath));
    }
}

