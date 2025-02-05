package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.l8group.videoeditor.requests.BatchProcessingRequest;
import com.l8group.videoeditor.requests.VideoConversionRequest;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.requests.VideoOverlayRequest;
import com.l8group.videoeditor.requests.VideoResizeRequest;

@Service
public class BatchProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessingService.class);
    private static final String OUTPUT_DIR = "processed-videos";

    private final VideoConversionService videoConversionService;
    private final VideoCutService videoCutService;
    private final VideoResizeService videoResizeService;
    private final VideoOverlayService videoOverlayService;
    private final ObjectMapper objectMapper;

    public BatchProcessingService(
            VideoConversionService videoConversionService,
            VideoCutService videoCutService,
            VideoResizeService videoResizeService,
            VideoOverlayService videoOverlayService,
            ObjectMapper objectMapper) {
        this.videoConversionService = videoConversionService;
        this.videoCutService = videoCutService;
        this.videoResizeService = videoResizeService;
        this.videoOverlayService = videoOverlayService;
        this.objectMapper = objectMapper;
    }

    public void processBatch(BatchProcessingRequest batchRequest) throws IOException, InterruptedException {
        List<String> videoIds = batchRequest.getVideoIds();
        createDirectoryIfNotExists(OUTPUT_DIR);

        for (String videoId : videoIds) {
            for (BatchProcessingRequest.BatchOperation operation : batchRequest.getOperations()) {
                String operationType = operation.getOperationType();
                Object parameters = operation.getParameters();

                try {
                    logger.info("Processando operação '{}' para o vídeo ID: {}", operationType, videoId);

                    switch (operationType.toUpperCase()) {
                        case "CONVERT" -> {
                            VideoConversionRequest conversionRequest = objectMapper.convertValue(parameters,
                                    VideoConversionRequest.class);
                            conversionRequest.setVideoId(videoId);
                            videoConversionService.convertVideo(conversionRequest);
                        }
                        case "CUT" -> {

                            VideoCutRequest cutRequest = objectMapper.convertValue(parameters, VideoCutRequest.class);
                            cutRequest.setVideoId(videoId);
                            videoCutService.cutVideo(cutRequest);
                        }
                        case "RESIZE" -> {
                            VideoResizeRequest resizeRequest = objectMapper.convertValue(parameters,
                                    VideoResizeRequest.class);
                            resizeRequest.setVideoId(videoId);
                            videoResizeService.resizeVideo(resizeRequest);
                        }
                        case "OVERLAY" -> {
                            VideoOverlayRequest overlayRequest = objectMapper.convertValue(parameters,
                                    VideoOverlayRequest.class);
                            overlayRequest.setVideoId(videoId);
                            videoOverlayService.createOverlay(overlayRequest);
                        }
                        default -> {
                            logger.error("Operação inválida: {}", operationType);
                            throw new IllegalArgumentException("Operação inválida: " + operationType);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Erro ao processar operação '{}' para o vídeo ID {}: {}", operationType, videoId,
                            e.getMessage(), e);
                }
            }
        }
    }

    private void createDirectoryIfNotExists(String path) {
        File directory = new File(path);
        if (!directory.exists() && directory.mkdirs()) {
            logger.info("Diretório criado: {}", path);
        }
    }
}
