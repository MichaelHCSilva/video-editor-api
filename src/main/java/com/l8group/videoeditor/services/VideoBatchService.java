package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.dtos.VideoBatchResponseDTO;
import com.l8group.videoeditor.enums.VideoStatusEnum;
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

@Service
public class VideoBatchService {

    private final VideoFileRepository videoFileRepository;
    private final VideoBatchProcessRepository videoBatchProcessRepository;
    private final VideoCutService videoCutService;
    private final VideoResizeService videoResizeService;
    private final VideoOverlayService videoOverlayService;
    private final VideoConversionService videoConversionService;
    private final VideoBatchProducer videoBatchProducer;

    private static final Logger logger = LoggerFactory.getLogger(VideoBatchService.class);

    @Value("${video.upload.dir}")
    private String UPLOAD_DIR;

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    @Autowired
    public VideoBatchService(VideoFileRepository videoFileRepository,
            VideoBatchProcessRepository videoBatchProcessRepository,
            VideoCutService videoCutService, VideoResizeService videoResizeService,
            VideoOverlayService videoOverlayService, VideoConversionService videoConversionService, VideoBatchProducer videoBatchProducer) {
        this.videoFileRepository = videoFileRepository;
        this.videoBatchProcessRepository = videoBatchProcessRepository;
        this.videoCutService = videoCutService;
        this.videoResizeService = videoResizeService;
        this.videoOverlayService = videoOverlayService;
        this.videoConversionService = videoConversionService;
        this.videoBatchProducer = videoBatchProducer;
    }

    @Transactional
    public VideoBatchResponseDTO processBatch(VideoBatchRequest request) {
        logger.info("🟢 [processBatch] Iniciando processamento em lote | Vídeos: {} | Operações: {}",
                request.getVideoIds(), request.getOperations());

        List<String> intermediateFiles = new ArrayList<>();

        try {
            createTempDirectory();

            UUID firstVideoId = UUID.fromString(request.getVideoIds().get(0));

            VideoFile originalVideoFile = videoFileRepository.findById(firstVideoId)
                    .orElseThrow(() -> {
                        logger.error("❌ [processBatch] Vídeo original não encontrado: {}", firstVideoId);
                        return new RuntimeException("Vídeo original não encontrado.");
                    });

            String currentInputFilePath = Paths.get(UPLOAD_DIR, originalVideoFile.getVideoFileName()).toString();
            String outputFormat = originalVideoFile.getVideoFileFormat().replace(".", "");

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

                } catch (Exception e) {
                    logger.error("❌ [processBatch] Erro ao processar operação: {}", operation.getOperationType(), e);
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

            VideoProcessingBatch batchProcess = createAndSaveBatchProcess(originalVideoFile, request.getOperations(),
                    finalOutputFileName);

            videoBatchProducer.sendVideoBatchId(batchProcess.getId());

            logger.info("✅ [processBatch] Processamento concluído | Vídeo ID: {} | Arquivo final: {}",
                    originalVideoFile.getId(), finalOutputPath);

            return new VideoBatchResponseDTO(
                    batchProcess.getId(), // ID do processamento em lote
                    finalOutputFileName,
                    batchProcess.getCreatedTimes(),
                    batchProcess.getProcessingSteps());

        } catch (Exception e) {
            logger.error("❌ [processBatch] Erro geral durante o processamento do lote", e);
            throw new RuntimeException("Erro geral no processamento do lote.", e);
        }
    }

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
            throw new RuntimeException("Erro ao executar operação.", e);
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
            List<VideoBatchRequest.BatchOperation> operations, String processedFileName) {
        VideoProcessingBatch batchProcess = new VideoProcessingBatch();
        batchProcess.setVideoFile(videoFile);
        batchProcess.setStatus(VideoStatusEnum.PROCESSING);
        batchProcess.setCreatedTimes(ZonedDateTime.now());
        batchProcess.setUpdatedTimes(ZonedDateTime.now());
        batchProcess.setVideoOutputFileName(processedFileName);
        batchProcess.setProcessingSteps(operations.stream().map(VideoBatchRequest.BatchOperation::getOperationType)
                .collect(Collectors.toList()));
        return videoBatchProcessRepository.save(batchProcess);
    }

    private String generateFinalOutputFileName(VideoFile videoFile, String outputFormat) {
        String shortUUID = VideoUtils.generateShortUuid();
        String formattedDate = VideoUtils.formatDateToCompactString(ZonedDateTime.now().toLocalDate());
        String originalFileName = videoFile.getVideoFileName().substring(0,
                videoFile.getVideoFileName().lastIndexOf(".")); // Remove
        return originalFileName + "_" + shortUUID + formattedDate + "_processed." + outputFormat;
    }
}
