package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.dtos.VideoBatchResponseDTO;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.metrics.VideoBatchServiceMetrics;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoProcessingBatch;
import com.l8group.videoeditor.rabbit.producer.VideoBatchProducer;
import com.l8group.videoeditor.repositories.VideoBatchProcessRepository;
import com.l8group.videoeditor.requests.VideoBatchRequest;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.utils.VideoFileStorageUtils;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoBatchService {

    private final VideoBatchProcessRepository videoBatchProcessRepository;
    private final VideoBatchProducer videoBatchProducer;
    private final VideoBatchServiceMetrics videoBatchServiceMetrics;
    private final VideoStatusManagerService videoStatusManagerService;
    private final VideoS3Service s3Service;
    private final VideoOperationExecutor videoOperationExecutor;
    private final VideoFileFinderService videoFileFinderService;

    @Value("${video.upload.dir}")
    private String UPLOAD_DIR;

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    @Transactional
    public VideoBatchResponseDTO processBatch(VideoBatchRequest request) throws IOException {
        log.info("[processBatch] Iniciando processamento em lote | Vídeos: {} | Operações: {}",
                request.getVideoIds(), request.getOperations());

        videoBatchServiceMetrics.incrementBatchRequests();
        videoBatchServiceMetrics.incrementProcessingQueueSize();

        Timer.Sample timerSample = videoBatchServiceMetrics.startBatchProcessingTimer();

        List<String> intermediateFiles = new ArrayList<>();
        VideoProcessingBatch batchProcess = null;
        VideoFile originalVideoFile = null;
        String outputFormat = null;
        String currentInputFilePath = null;

        try {
            VideoFileStorageUtils.createDirectoryIfNotExists(TEMP_DIR);
            videoOperationExecutor.validateAllOperations(request.getVideoIds().get(0), request.getOperations());

            originalVideoFile = videoFileFinderService.findById(request.getVideoIds().get(0));
            currentInputFilePath = VideoFileStorageUtils.buildFilePath(UPLOAD_DIR,
                    originalVideoFile.getVideoFileName());
            outputFormat = originalVideoFile.getVideoFileFormat().replace(".", "");

            batchProcess = new VideoProcessingBatch();
            batchProcess.setVideoFile(originalVideoFile);
            batchProcess.setStatus(VideoStatusEnum.PROCESSING);
            batchProcess.setCreatedTimes(ZonedDateTime.now());
            batchProcess.setUpdatedTimes(ZonedDateTime.now());
            batchProcess.setVideoFilePath(null);
            batchProcess.setProcessingSteps(request.getOperations().stream()
                    .map(VideoBatchRequest.BatchOperation::getOperationType).collect(Collectors.toList()));
            batchProcess = videoBatchProcessRepository.save(batchProcess); 

            for (VideoBatchRequest.BatchOperation operation : request.getOperations()) {
                log.info("🔹 [processBatch] Processando operação: {} | Input: {}", operation.getOperationType(),
                        currentInputFilePath);

                String nextOutputFilePath = videoOperationExecutor.execute(
                        request.getVideoIds().get(0),
                        List.of(operation), 
                        currentInputFilePath,
                        outputFormat);

                if (nextOutputFilePath != null) {
                    if (!new File(nextOutputFilePath).exists()) {
                        log.error("[processBatch] Arquivo de saída não encontrado após operação: {}",
                                nextOutputFilePath);
                        throw new RuntimeException("Arquivo de saída não encontrado após operação.");
                    }

                    if (currentInputFilePath.startsWith(TEMP_DIR)) {
                        intermediateFiles.add(currentInputFilePath);
                    }

                    currentInputFilePath = nextOutputFilePath;

                    int lastDot = nextOutputFilePath.lastIndexOf(".");
                    if (lastDot > 0) {
                        outputFormat = nextOutputFilePath.substring(lastDot + 1);
                    }
                }
            }

            String finalOutputFileName = VideoFileNameGenerator
                    .generateFileNameWithSuffix(originalVideoFile.getVideoFileName(), "PROCESSED");

            int dotIndex = finalOutputFileName.lastIndexOf(".");
            if (dotIndex != -1) {
                finalOutputFileName = finalOutputFileName.substring(0, dotIndex) + "." + outputFormat;
            }

            Path finalOutputPath = Paths.get(TEMP_DIR, finalOutputFileName);
            try {
                VideoFileStorageUtils.moveFile(Paths.get(currentInputFilePath), finalOutputPath);
            } catch (IOException e) {
                log.error("Erro ao mover o arquivo final para o diretório temporário: {}", e.getMessage());
                throw new RuntimeException("Erro ao mover o arquivo final para o diretório temporário", e);
            }

            intermediateFiles.forEach(filePath -> VideoFileStorageUtils.deleteFileIfExists(new File(filePath)));

            videoBatchProducer.sendVideoBatchId(batchProcess.getId());

            s3Service.uploadProcessedFile(finalOutputPath.toFile(), finalOutputFileName, originalVideoFile.getId());
            batchProcess.setVideoFilePath(s3Service.getFileUrl(VideoS3Service.PROCESSED_VIDEO_FOLDER, finalOutputFileName));
            videoBatchProcessRepository.save(batchProcess);

            videoStatusManagerService.updateEntityStatus(videoBatchProcessRepository, batchProcess.getId(),
                    VideoStatusEnum.COMPLETED, "processBatch - Conclusão");

            videoBatchServiceMetrics.recordBatchProcessingDuration(timerSample);
            videoBatchServiceMetrics.incrementBatchSuccess();
            videoBatchServiceMetrics.decrementProcessingQueueSize();
            videoBatchServiceMetrics.setProcessedFileSize(finalOutputPath.toFile().length());

            log.info("[processBatch] Processamento concluído | Batch ID: {} | Arquivo final: {}",
                    batchProcess.getId(), finalOutputPath);

            return new VideoBatchResponseDTO(batchProcess.getId(), finalOutputFileName, batchProcess.getCreatedTimes(),
                    batchProcess.getProcessingSteps());

        } catch (Exception e) {
            videoBatchServiceMetrics.incrementBatchFailure();
            videoBatchServiceMetrics.decrementProcessingQueueSize();
            if (batchProcess != null) {
                videoStatusManagerService.updateEntityStatus(videoBatchProcessRepository, batchProcess.getId(),
                        VideoStatusEnum.ERROR, "processBatch - Falha");
            }
            throw e;
        } finally {
            videoBatchServiceMetrics.decrementProcessingQueueSize();
        }
    }
}