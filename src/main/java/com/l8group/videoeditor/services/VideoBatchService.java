package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.dtos.VideoBatchResponseDTO;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.InvalidCutTimeException;
import com.l8group.videoeditor.exceptions.InvalidResizeParameterException;
import com.l8group.videoeditor.exceptions.InvalidVideoIdListException; // Importe a nova exceção
import com.l8group.videoeditor.exceptions.VideoNotFoundException;
import com.l8group.videoeditor.metrics.VideoBatchServiceMetrics;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoProcessingBatch;
import com.l8group.videoeditor.rabbit.producer.VideoBatchProducer;
import com.l8group.videoeditor.repositories.VideoBatchProcessRepository;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoBatchRequest;
import com.l8group.videoeditor.requests.VideoConversionRequest;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.requests.VideoOverlayRequest;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.utils.VideoUtils;
import com.l8group.videoeditor.validation.VideoConversionValidator;
import com.l8group.videoeditor.validation.VideoResizeValidator;

import io.micrometer.core.instrument.Timer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoBatchService {

    private final VideoFileRepository videoFileRepository;
    private final VideoBatchProcessRepository videoBatchProcessRepository;
    private final VideoCutService videoCutService;
    private final VideoResizeService videoResizeService;
    private final VideoOverlayService videoOverlayService;
    private final VideoConversionService videoConversionService;
    private final VideoBatchProducer videoBatchProducer;
    private final VideoBatchServiceMetrics videoBatchServiceMetrics;
    private final VideoStatusManagerService videoStatusManagerService;
    private final S3Service s3Service;
    private final Validator validator;
    private final VideoConversionValidator videoConversionValidator;

    private static final Logger logger = LoggerFactory.getLogger(VideoBatchService.class);

    @Value("${video.upload.dir}")
    private String UPLOAD_DIR;

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    @Transactional
    public VideoBatchResponseDTO processBatch(VideoBatchRequest request) {
        logger.info("🟢 [processBatch] Iniciando processamento em lote | Vídeos: {} | Operações: {}",
                request.getVideoIds(), request.getOperations());

        videoBatchServiceMetrics.incrementBatchRequests();
        videoBatchServiceMetrics.incrementProcessingQueueSize();

        Timer.Sample timerSample = videoBatchServiceMetrics.startBatchProcessingTimer();

        List<String> intermediateFiles = new ArrayList<>();

        try {
            createTempDirectory();

            if (request.getVideoIds() == null || request.getVideoIds().isEmpty()) {
                logger.error("❌ [processBatch] A lista de videoIds não pode estar vazia.");
                throw new InvalidVideoIdListException("A lista de IDs de vídeo não pode estar vazia.");
            }

            for (String videoId : request.getVideoIds()) {
                if (videoId == null || videoId.trim().isEmpty()) {
                    logger.error("❌ [processBatch] ID de vídeo inválido: {}", videoId);
                    throw new InvalidVideoIdListException("Insira um ID de vídeo válido.");
                }
                try {
                    UUID.fromString(videoId); // Tenta converter para UUID para validar o formato básico
                } catch (IllegalArgumentException e) {
                    logger.error("❌ [processBatch] Formato de ID de vídeo inválido: {}", videoId);
                    throw new InvalidVideoIdListException("O ID de vídeo '" + videoId + "' possui um formato inválido.");
                }
            }

            UUID firstVideoId = UUID.fromString(request.getVideoIds().get(0));
            VideoFile originalVideoFile = videoFileRepository.findById(firstVideoId)
                    .orElseThrow(() -> new VideoNotFoundException(firstVideoId));

            String currentInputFilePath = Paths.get(UPLOAD_DIR, originalVideoFile.getVideoFileName()).toString();
            String outputFormat = originalVideoFile.getVideoFileFormat().replace(".", "");

            VideoProcessingBatch batchProcess = createAndSaveBatchProcess(originalVideoFile, request.getOperations());

            for (VideoBatchRequest.BatchOperation operation : request.getOperations()) {
                if ("CONVERT".equalsIgnoreCase(operation.getOperationType())
                        && operation.getParameters().getOutputFormat() != null
                        && !operation.getParameters().getOutputFormat().isEmpty()) {
                    outputFormat = operation.getParameters().getOutputFormat();
                }

                logger.info("🔹 [processBatch] Processando operação: {} | Input: {}",
                        operation.getOperationType(), currentInputFilePath);

                try {
                    String outputFilePath = executeOperation(request.getVideoIds().get(0), operation,
                            operation.getParameters(),
                            outputFormat, currentInputFilePath);

                    if (outputFilePath != null) {
                        File outputFile = new File(outputFilePath);
                        if (!outputFile.exists()) {
                            logger.error("❌ [processBatch] Arquivo de saída não encontrado após operação: {}",
                                    outputFilePath);
                            throw new RuntimeException("Arquivo de saída não encontrado após operação.");
                        }
                        if (currentInputFilePath.startsWith(TEMP_DIR)) {
                            intermediateFiles.add(currentInputFilePath);
                        }
                        currentInputFilePath = outputFilePath;
                    }

                } catch (InvalidCutTimeException | IllegalArgumentException | InvalidResizeParameterException e) {
                    throw e; // Relança as exceções específicas
                } catch (Exception e) {
                    logger.error("❌ [processBatch] Erro ao processar operação: {}", operation.getOperationType(), e);
                    videoBatchServiceMetrics.incrementBatchFailure();
                    throw new RuntimeException("Falha ao processar operação.", e);
                }
            }

            String finalOutputFileName = generateFinalOutputFileName(originalVideoFile, outputFormat);
            String finalOutputPath = Paths.get(TEMP_DIR, finalOutputFileName).toString();

            logger.info("🟠 [processBatch] Renomeando arquivo final: {} para: {}", currentInputFilePath,
                    finalOutputPath);

            try {
                Files.move(Paths.get(currentInputFilePath), Paths.get(finalOutputPath));
            } catch (IOException e) {
                logger.error("❌ [processBatch] Erro ao renomear arquivo final", e);
                throw new RuntimeException("Falha ao renomear arquivo final.", e);
            }

            deleteIntermediateFiles(intermediateFiles);

            videoBatchProducer.sendVideoBatchId(batchProcess.getId());

            s3Service.uploadProcessedFile(new File(finalOutputPath), finalOutputFileName, originalVideoFile.getId());
            String s3FileUrl = s3Service.getFileUrl(S3Service.PROCESSED_VIDEO_FOLDER, finalOutputFileName);
            batchProcess.setVideoFilePath(s3FileUrl);
            videoBatchProcessRepository.save(batchProcess);

            videoStatusManagerService.updateEntityStatus(videoBatchProcessRepository, batchProcess.getId(),
                    VideoStatusEnum.COMPLETED, "processBatch");

            videoBatchServiceMetrics.recordBatchProcessingDuration(timerSample);
            videoBatchServiceMetrics.incrementBatchSuccess();
            videoBatchServiceMetrics.decrementProcessingQueueSize();

            long processedFileSize = new File(finalOutputPath).length();
            videoBatchServiceMetrics.setProcessedFileSize(processedFileSize);

            logger.info("✅ [processBatch] Processamento concluído | Vídeo ID: {} | Arquivo final: {}",
                    originalVideoFile.getId(), finalOutputPath);

            return new VideoBatchResponseDTO(
                    batchProcess.getId(),
                    finalOutputFileName,
                    batchProcess.getCreatedTimes(),
                    batchProcess.getProcessingSteps());

        } catch (InvalidVideoIdListException e) {
            // Captura a exceção específica para lista de IDs inválida
            videoBatchServiceMetrics.incrementBatchFailure();
            videoBatchServiceMetrics.decrementProcessingQueueSize();
            throw e;
        } catch (IOException e) {
            log.error("❌ [processBatch] Erro geral de I/O durante o processamento do lote", e);
            videoBatchServiceMetrics.incrementBatchFailure();
            videoBatchServiceMetrics.decrementProcessingQueueSize();
            throw new RuntimeException("Erro de I/O durante o processamento do lote.", e);
        } catch (Exception e) {
            log.error("❌ [processBatch] Erro geral durante o processamento do lote", e);
            videoBatchServiceMetrics.incrementBatchFailure();
            videoBatchServiceMetrics.decrementProcessingQueueSize();
            throw e;
        }

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private String executeOperation(String videoId,
                                     VideoBatchRequest.BatchOperation operation,
                                     VideoBatchRequest.OperationParameters parameters,
                                     String outputFormat,
                                     String currentInputFilePath) {

        logger.info("🛠️ [executeOperation] Executando operação: {} | Video ID: {} | Input: {}",
                operation.getOperationType(), videoId, currentInputFilePath);

        try {
            String outputFilePath = switch (operation.getOperationType().toUpperCase()) {
                case "CUT" -> {
                    VideoCutRequest cutRequest = new VideoCutRequest();
                    cutRequest.setVideoId(videoId);
                    cutRequest.setStartTime(parameters.getStartTime());
                    cutRequest.setEndTime(parameters.getEndTime());
                    Set<ConstraintViolation<VideoCutRequest>> violations = validator.validate(cutRequest);
                    if (!violations.isEmpty()) {
                        String errorMessages = violations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining(", "));
                        throw new IllegalArgumentException("Erros na requisição de corte: " + errorMessages);
                    }
                    yield videoCutService.cutVideo(cutRequest, currentInputFilePath);
                }
                case "RESIZE" -> {
                    Integer width = parseInteger(parameters.getWidth());
                    String heightStr = parameters.getHeight();

                    try {
                        VideoResizeValidator.validateNumericWidthAndHeight(parameters.getWidth(), heightStr);
                    } catch (InvalidResizeParameterException e) {
                        throw e;
                    }

                    Integer height = parseInteger(heightStr);
                    VideoResizeRequest resizeRequest = new VideoResizeRequest(videoId, width, height);

                    Set<ConstraintViolation<VideoResizeRequest>> violations = validator.validate(resizeRequest);
                    if (!violations.isEmpty()) {
                        String errorMessages = violations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining(", "));
                        throw new IllegalArgumentException("Erros na requisição de redimensionamento: " + errorMessages);
                    }
                    yield videoResizeService.resizeVideo(resizeRequest, currentInputFilePath);
                }
                case "OVERLAY" -> {
                    VideoOverlayRequest overlayRequest = new VideoOverlayRequest();
                    overlayRequest.setVideoId(videoId);
                    overlayRequest.setWatermark(parameters.getWatermark());
                    overlayRequest.setPosition(parameters.getPosition()); // Agora String
                    overlayRequest.setFontSize(parameters.getFontSize());
                    Set<ConstraintViolation<VideoOverlayRequest>> violations = validator.validate(overlayRequest);
                    if (!violations.isEmpty()) {
                        String errorMessages = violations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining(", "));
                        throw new IllegalArgumentException("Erros na requisição de overlay: " + errorMessages);
                    }
                    yield videoOverlayService.processOverlay(overlayRequest, currentInputFilePath);
                }
                case "CONVERT" -> {
                    VideoConversionRequest convertRequest = new VideoConversionRequest();
                    convertRequest.setVideoId(videoId);
                    convertRequest.setOutputFormat(parameters.getOutputFormat()); // Usar o outputFormat do parameters

                    // Validar o formato de saída usando VideoConversionValidator
                    videoConversionValidator.validateOutputFormat(convertRequest.getOutputFormat());

                    yield videoConversionService.convertVideo(convertRequest, currentInputFilePath);
                }
                default -> throw new IllegalArgumentException("Operação inválida: " + operation.getOperationType()+ ". Os tipos suportados são: CUT, RESIZE, CONVERT e OVERLAY.");
            };

            logger.info("✅ [executeOperation] Operação concluída: {} | Arquivo de saída: {}",
                    operation.getOperationType(), outputFilePath);
            return outputFilePath;

        } catch (InvalidResizeParameterException e) {
            logger.error("❌ [executeOperation] Erro de validação de redimensionamento: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("❌ [executeOperation] Erro de argumento inválido: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("❌ [executeOperation] Erro ao executar operação: {}", operation.getOperationType(), e);
            throw new RuntimeException("Falha ao processar operação.", e);
        }
    }

    private void createTempDirectory() {
        logger.info("🛠️ [createTempDirectory] Verificando ou criando diretório temporário: {}", TEMP_DIR);
        Path tempDirPath = Paths.get(TEMP_DIR);
        if (!Files.exists(tempDirPath)) {
            try {
                Files.createDirectories(tempDirPath);
                logger.info("📁 [createTempDirectory] Diretório temporário criado: {}", TEMP_DIR);
            } catch (IOException e) {
                logger.error("❌ [createTempDirectory] Erro ao criar diretório temporário", e);
                throw new RuntimeException("Erro ao criar diretório temporário.", e);
            }
        }
    }

    private void deleteIntermediateFiles(List<String> intermediateFiles) {
        for (String filePath : intermediateFiles) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
                logger.info("🗑️ [deleteIntermediateFiles] Arquivo intermediário deletado: {}", filePath);
            } catch (IOException e) {
                logger.error("❌ [deleteIntermediateFiles] Erro ao excluir arquivo intermediário: {}", filePath, e);
            }
        }
    }

    private VideoProcessingBatch createAndSaveBatchProcess(VideoFile videoFile,
                                                                 List<VideoBatchRequest.BatchOperation> operations) {
        VideoProcessingBatch batchProcess = new VideoProcessingBatch();
        batchProcess.setVideoFile(videoFile);
        batchProcess.setStatus(VideoStatusEnum.PROCESSING);
        batchProcess.setCreatedTimes(ZonedDateTime.now());
        batchProcess.setUpdatedTimes(ZonedDateTime.now());
        batchProcess.setVideoFilePath(null);
        batchProcess.setProcessingSteps(operations.stream().map(VideoBatchRequest.BatchOperation::getOperationType)
                .collect(Collectors.toList()));
        return videoBatchProcessRepository.save(batchProcess);
    }

    private String generateFinalOutputFileName(VideoFile videoFile, String outputFormat) {
        String shortUUID = VideoUtils.generateShortUuid();
        String formattedDate = VideoUtils.formatDateToCompactString(ZonedDateTime.now().toLocalDate());
        String originalFileName = videoFile.getVideoFileName().substring(0,
                videoFile.getVideoFileName().lastIndexOf("."));
        return originalFileName + "_" + shortUUID + formattedDate + "_processed." + outputFormat;
    }

    private Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}