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
    private final Validator validator; // Injete o Validator

    private static final Logger logger = LoggerFactory.getLogger(VideoBatchService.class);

    @Value("${video.upload.dir}")
    private String UPLOAD_DIR;

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    @Transactional
    public VideoBatchResponseDTO processBatch(VideoBatchRequest request) {
        logger.info("🟢 [processBatch] Iniciando processamento em lote | Vídeos: {} | Operações: {}",
                request.getVideoIds(), request.getOperations());

        videoBatchServiceMetrics.incrementBatchRequests(); // ⬅️ Registra uma nova solicitação
        videoBatchServiceMetrics.incrementProcessingQueueSize(); // ⬅️ Incrementa a fila de processamento

        Timer.Sample timerSample = videoBatchServiceMetrics.startBatchProcessingTimer(); // ⬅️ Inicia a medição do tempo

        List<String> intermediateFiles = new ArrayList<>();

        try {
            createTempDirectory();

            // Validação se a lista de videoIds está vazia
            if (request.getVideoIds().isEmpty()) {
                logger.error("❌ [processBatch] A lista de videoIds não pode estar vazia.");
                throw new IllegalArgumentException("A lista de videoIds não pode estar vazia.");
            }

            UUID firstVideoId;
            try {
                firstVideoId = UUID.fromString(request.getVideoIds().get(0));
            } catch (IllegalArgumentException e) {
                logger.error("❌ [processBatch] Formato de ID de vídeo inválido: {}", request.getVideoIds().get(0));
                throw new IllegalArgumentException("Formato dO ID de vídeo inválido: " + request.getVideoIds().get(0));
            }

            VideoFile originalVideoFile = videoFileRepository.findById(firstVideoId)
                    .orElseThrow(() -> {
                        logger.error("❌ [processBatch] Vídeo original não encontrado: {}", firstVideoId);
                        return new RuntimeException("Vídeo original não encontrado.");
                    });

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

                        // Adiciona apenas os arquivos da pasta TEMP para exclusão
                        if (currentInputFilePath.startsWith(TEMP_DIR)) {
                            intermediateFiles.add(currentInputFilePath);
                        }

                        currentInputFilePath = outputFilePath;
                    }

                } catch (InvalidCutTimeException | IllegalArgumentException e) {
                    throw e; // Relança ambas as exceções
                } catch (Exception e) {
                    logger.error("❌ [processBatch] Erro ao processar operação: {}", operation.getOperationType(), e);
                    videoBatchServiceMetrics.incrementBatchFailure(); // ⬅️ Incrementa falhas
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

            // Upload do arquivo final para o S3 na pasta processed-videos
            s3Service.uploadProcessedFile(new File(finalOutputPath), finalOutputFileName, originalVideoFile.getId());

            // Obter a URL do arquivo no S3
            String s3FileUrl = s3Service.getFileUrl(S3Service.PROCESSED_VIDEO_FOLDER, finalOutputFileName);

            // Atualizar o videoFilePath no VideoProcessingBatch
            batchProcess.setVideoFilePath(s3FileUrl);
            videoBatchProcessRepository.save(batchProcess); // Salvar a atualização

            videoStatusManagerService.updateEntityStatus(videoBatchProcessRepository, batchProcess.getId(),
                    VideoStatusEnum.COMPLETED, "processBatch");

            videoBatchServiceMetrics.recordBatchProcessingDuration(timerSample); // ⬅️ Registra o tempo de processamento
            videoBatchServiceMetrics.incrementBatchSuccess(); // ⬅️ Incrementa sucessos
            videoBatchServiceMetrics.decrementProcessingQueueSize(); // ⬅️ Decrementa a fila de processamento

            long processedFileSize = new File(finalOutputPath).length();
            videoBatchServiceMetrics.setProcessedFileSize(processedFileSize); // ⬅️ Define o tamanho do arquivo final

            logger.info("✅ [processBatch] Processamento concluído | Vídeo ID: {} | Arquivo final: {}",
                    originalVideoFile.getId(), finalOutputPath);

            return new VideoBatchResponseDTO(
                    batchProcess.getId(), // ID do processamento em lote
                    finalOutputFileName,
                    batchProcess.getCreatedTimes(),
                    batchProcess.getProcessingSteps());

        } catch (IOException e) {
            log.error("❌ [processBatch] Erro geral de I/O durante o processamento do lote", e);
            videoBatchServiceMetrics.incrementBatchFailure();
            videoBatchServiceMetrics.decrementProcessingQueueSize();
            throw new RuntimeException("Erro de I/O durante o processamento do lote.", e);
        } catch (Exception e) {
            log.error("❌ [processBatch] Erro geral durante o processamento do lote", e);
            videoBatchServiceMetrics.incrementBatchFailure();
            videoBatchServiceMetrics.decrementProcessingQueueSize();
            throw e; // Relança a exceção original para ser tratada por camadas superiores
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

                    // Validar o VideoCutRequest ANTES de chamar o VideoCutService
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
                    VideoResizeRequest resizeRequest = new VideoResizeRequest();
                    resizeRequest.setVideoId(videoId);
                    resizeRequest.setWidth(parameters.getWidth());
                    resizeRequest.setHeight(parameters.getHeight());
                    yield videoResizeService.resizeVideo(resizeRequest, currentInputFilePath);
                }
                case "OVERLAY" -> {
                    VideoOverlayRequest overlayRequest = new VideoOverlayRequest();
                    overlayRequest.setVideoId(videoId);
                    overlayRequest.setWatermark(parameters.getWatermark());
                    overlayRequest.setPosition(parameters.getPosition());
                    overlayRequest.setFontSize(parameters.getFontSize());
                    yield videoOverlayService.processOverlay(overlayRequest, currentInputFilePath);
                }
                case "CONVERT" -> {
                    VideoConversionRequest convertRequest = new VideoConversionRequest();
                    convertRequest.setVideoId(videoId);
                    convertRequest.setOutputFormat(outputFormat);
                    yield videoConversionService.convertVideo(convertRequest, currentInputFilePath);
                }
                default -> throw new IllegalArgumentException("Operação inválida: " + operation.getOperationType());
            };

            logger.info("✅ [executeOperation] Operação concluída: {} | Arquivo de saída: {}",
                    operation.getOperationType(), outputFilePath);
            return outputFilePath;

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
        batchProcess.setVideoFilePath(null); // valor inicial é nulo
        batchProcess.setProcessingSteps(operations.stream().map(VideoBatchRequest.BatchOperation::getOperationType)
                .collect(Collectors.toList()));
        return videoBatchProcessRepository.save(batchProcess);
    }

    private String generateFinalOutputFileName(VideoFile videoFile, String outputFormat) {
        String shortUUID = VideoUtils.generateShortUuid();
        String formattedDate = VideoUtils.formatDateToCompactString(ZonedDateTime.now().toLocalDate());
        String originalFileName = videoFile.getVideoFileName().substring(0,
                videoFile.getVideoFileName().lastIndexOf(".")); // Remove a extensão
        return originalFileName + "_" + shortUUID + formattedDate + "_processed." + outputFormat;
    }
}