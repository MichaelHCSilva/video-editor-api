package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.metrics.VideoResizeServiceMetrics;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoResize;
import com.l8group.videoeditor.rabbit.producer.VideoResizeProducer;
import com.l8group.videoeditor.repositories.VideoResizeRepository;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.utils.VideoFileStorageUtils;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.utils.VideoProcessorUtils;
//import com.l8group.videoeditor.utils.VideoResolutionsUtils;
import com.l8group.videoeditor.validation.VideoResizeValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoResizeService {

    private final VideoResizeRepository videoResizeRepository;
    private final VideoResizeProducer videoResizeProducer;
    private final VideoResizeServiceMetrics videoResizeServiceMetrics;
    private final VideoFileFinderService videoFileFinderService;

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    @Transactional
    public String resizeVideo(VideoResizeRequest request, String previousFilePath) {
        videoResizeServiceMetrics.incrementResizeRequests();

        // Chamando o novo validador correto
        VideoResizeValidator.validate(request.getWidth(), request.getHeight());

        UUID videoId = UUID.fromString(request.getVideoId());

        VideoFile videoFile = videoFileFinderService.findById(videoId);

        String inputFilePath = previousFilePath != null ? previousFilePath : videoFile.getVideoFilePath();

        validateInputFile(inputFilePath);

        String outputFilePath = prepareOutputFile(videoFile.getVideoFileName());

        try {
            boolean success = VideoProcessorUtils.resizeVideo(
                    inputFilePath, outputFilePath, request.getWidth(), request.getHeight());

            if (!success) {
                throw new VideoProcessingException("Erro ao redimensionar vídeo.");
            }

            try {
                long fileSize = Files.size(Paths.get(outputFilePath));
                videoResizeServiceMetrics.setResizeFileSize(fileSize);
                log.info("Tamanho do vídeo redimensionado: {} bytes", fileSize);
            } catch (Exception e) {
                log.error("Erro ao obter o tamanho do arquivo de saída: {}", e.getMessage());
            }

        } catch (Exception e) {
            throw new VideoProcessingException("Erro inesperado ao redimensionar vídeo.", e);
        }

        saveResizeEntity(videoFile, request);

        videoResizeProducer.sendMessage(videoId.toString());

        return outputFilePath;
    }

    /*private void validateRequestResolution(VideoResizeRequest request) {
        if (!VideoResolutionsUtils.isValidResolution(request.getWidth(), request.getHeight())) {
            String supported = VideoResolutionsUtils.getSupportedResolutionsAsString();
            log.error("Resolução inválida: {}x{}. Resoluções suportadas: {}", request.getWidth(), request.getHeight(),
                    supported);
            throw new VideoProcessingException(
                    String.format("Resolução não suportada: %dx%d. Suportadas: %s",
                            request.getWidth(), request.getHeight(), supported));
        }
    }*/

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
                TEMP_DIR, VideoFileNameGenerator.generateFileNameWithSuffix(originalFileName, "resize"));
    }

    private void saveResizeEntity(VideoFile videoFile, VideoResizeRequest request) {
        VideoResize resize = new VideoResize();
        resize.setVideoFile(videoFile);
        resize.setTargetResolution(request.getWidth() + "x" + request.getHeight());
        resize.setStatus(VideoStatusEnum.PROCESSING);
        resize.setCreatedTimes(ZonedDateTime.now());
        resize.setUpdatedTimes(ZonedDateTime.now());
        videoResizeRepository.save(resize);
    }

    public void deleteTemporaryFiles(String filePath) {
        VideoFileStorageUtils.deleteFileIfExists(new File(filePath));
    }
}
