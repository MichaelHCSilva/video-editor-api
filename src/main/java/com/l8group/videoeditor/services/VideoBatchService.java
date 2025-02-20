package com.l8group.videoeditor.services;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.dtos.VideoBatchResponseDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoBatchProcess;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoBatchProcessRepository;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoBatchRequest;
import com.l8group.videoeditor.requests.VideoConversionRequest;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.requests.VideoResizeRequest;

@Service
public class VideoBatchService {

    private final VideoFileService videoFileService;
    private final VideoBatchProcessRepository videoBatchProcessRepository;
    private final VideoFileRepository videoFileRepository;
    private final VideoCutService videoCutService;
    private final VideoResizeService videoResizeService;
    private final VideoConversionService videoConversionService;

    public VideoBatchService(VideoFileService videoFileService,
            VideoBatchProcessRepository videoBatchProcessRepository,
            VideoFileRepository videoFileRepository,
            VideoCutService videoCutService,
            VideoResizeService videoResizeService,
            VideoConversionService videoConversionService) {
        this.videoFileService = videoFileService;
        this.videoBatchProcessRepository = videoBatchProcessRepository;
        this.videoFileRepository = videoFileRepository;
        this.videoCutService = videoCutService;
        this.videoResizeService = videoResizeService;
        this.videoConversionService = videoConversionService;
    }

    @Transactional
    public VideoBatchResponseDTO processBatch(VideoBatchRequest request) {
        List<VideoFile> videoFiles = request.getVideoIds().stream()
                .map(UUID::fromString)
                .map(videoFileService::getVideoById)
                .collect(Collectors.toList());

        if (videoFiles.isEmpty()) {
            throw new RuntimeException("Nenhum dos vídeos informados foi encontrado.");
        }

        for (VideoFile videoFile : videoFiles) {
            String inputFilePath = videoFile.getFilePath();

            for (VideoBatchRequest.BatchOperation operation : request.getOperations()) {
                VideoBatchRequest.OperationParameters params = operation.getParameters();

                try {
                    inputFilePath = switch (operation.getOperationType().toUpperCase()) {
                        case "CUT" -> {
                            if (params.getStartTime() == null || params.getEndTime() == null) {
                                throw new IllegalArgumentException("Parâmetros inválidos para CUT.");
                            }

                            VideoCutRequest cutRequest = new VideoCutRequest();
                            cutRequest.setVideoId(videoFile.getId().toString());
                            cutRequest.setStartTime(params.getStartTime());
                            cutRequest.setEndTime(params.getEndTime());

                            videoCutService.cutVideo(cutRequest);
                            yield inputFilePath;
                        }

                        case "RESIZE" -> {
                            if (params.getWidth() == null || params.getHeight() == null) {
                                throw new IllegalArgumentException("Parâmetros inválidos para RESIZE.");
                            }

                            VideoResizeRequest resizeRequest = new VideoResizeRequest();
                            resizeRequest.setVideoId(videoFile.getId().toString());
                            resizeRequest.setWidth(params.getWidth());
                            resizeRequest.setHeight(params.getHeight());

                            videoResizeService.resizeVideo(resizeRequest);
                            yield inputFilePath;
                        }

                        case "CONVERT" -> {
                            if (params.getFormat() == null || params.getFormat().isEmpty()) {
                                throw new IllegalArgumentException("Parâmetro inválido para CONVERT.");
                            }

                            VideoConversionRequest conversionRequest = new VideoConversionRequest();
                            conversionRequest.setVideoId(videoFile.getId().toString());
                            conversionRequest.setFormat(params.getFormat());

                            videoConversionService.convertVideo(conversionRequest);
                            yield inputFilePath.replaceFirst("\\.[^.]+$", "") + "." + params.getFormat();
                        }

                        default ->
                            throw new IllegalArgumentException("Operação inválida: " + operation.getOperationType());
                    };
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(
                            "Erro ao processar operação " + operation.getOperationType() + ": " + e.getMessage(), e);
                }
            }

            // Atualiza o caminho do vídeo no banco de dados
            videoFile.setFilePath(inputFilePath);
            videoFileRepository.save(videoFile);

            VideoBatchProcess batchProcess = new VideoBatchProcess();
            batchProcess.setVideoFile(videoFile);
            batchProcess.setStatus(VideoStatus.PROCESSING);
            batchProcess.setCreatedAt(ZonedDateTime.now());
            batchProcess.setUpdatedAt(ZonedDateTime.now());

            String operationsString = request.getOperations().stream()
                    .map(VideoBatchRequest.BatchOperation::getOperationType)
                    .collect(Collectors.joining(" - "));

            batchProcess.setOperations(Arrays.asList(operationsString.split(" - ")));

            videoBatchProcessRepository.saveAndFlush(batchProcess);

            String fileName = Paths.get(inputFilePath).getFileName().toString();

            return new VideoBatchResponseDTO(
                batchProcess.getId(), 
                fileName,
                batchProcess.getCreatedAt(),
                request.getOperations().stream()
                        .map(VideoBatchRequest.BatchOperation::getOperationType)
                        .collect(Collectors.toList())
        );
        }

        throw new RuntimeException("Erro inesperado no processamento do lote.");
    }
}
