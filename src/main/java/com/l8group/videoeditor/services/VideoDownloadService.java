package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.ProcessedFileNotFoundException;
import com.l8group.videoeditor.metrics.VideoDownloadMetrics;
import com.l8group.videoeditor.models.VideoDownload;
import com.l8group.videoeditor.models.VideoProcessingBatch;
import com.l8group.videoeditor.rabbit.producer.VideoDownloadProducer;
import com.l8group.videoeditor.repositories.VideoDownloadRepository;
import com.l8group.videoeditor.validation.VideoDownloadValidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import io.micrometer.core.instrument.Timer;

import java.net.URI;
import java.time.ZonedDateTime;

@Service
public class VideoDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadService.class);

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private final S3Client s3Client;
    private final VideoBatchFinderService videoProcessingBatchFinderService;
    private final VideoDownloadValidation requestValidator;
    private final VideoDownloadMetrics videoDownloadMetrics;
    private final VideoDownloadRepository videoDownloadRepository;
    private final VideoDownloadProducer videoDownloadProducer;
    private final VideoStatusService videoStatusManagerService;

    public VideoDownloadService(
            VideoBatchFinderService finderService,
            VideoDownloadValidation requestValidator,
            VideoDownloadMetrics videoDownloadMetrics,
            VideoDownloadRepository videoDownloadRepository,
            VideoDownloadProducer videoDownloadProducer,
            VideoStatusService videoStatusManagerService) {

        this.videoProcessingBatchFinderService = finderService;
        this.requestValidator = requestValidator;
        this.videoDownloadMetrics = videoDownloadMetrics;
        this.videoDownloadRepository = videoDownloadRepository;
        this.videoDownloadProducer = videoDownloadProducer;
        this.videoStatusManagerService = videoStatusManagerService;

        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create("editor-video-s3"))
                .build();

        logger.info("S3 Client configurado para a região: {}", Region.US_EAST_1.id());
        logger.info("VideoDownloadService inicializado.");
    }

    public ResponseEntity<InputStreamResource> downloadVideoStreamFromS3(String rawBatchProcessId) {
        logger.info("Iniciando download para batchProcessId: {}", rawBatchProcessId);
        videoDownloadMetrics.incrementDownloadRequests();
        Timer.Sample timer = videoDownloadMetrics.startDownloadTimer();

        VideoDownload savedDownload = null;

        try {
            requestValidator.validateRawBatchProcessId(rawBatchProcessId);
            VideoProcessingBatch batch = videoProcessingBatchFinderService.findById(rawBatchProcessId);
            requestValidator.validateVideoProcessingBatch(batch);

            String filePath = batch.getS3Url();
            logger.info("URL obtida do batch: {}", filePath);

            String key = extractS3Key(filePath);
            logger.info("Chave S3 extraída: {}", key);

            String downloadFileName = key.substring(key.lastIndexOf('/') + 1);
            logger.info("Nome do arquivo: {}", downloadFileName);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            long contentLength = response.response().contentLength();
            InputStreamResource resource = new InputStreamResource(response);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadFileName + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, detectMimeType(downloadFileName));
            headers.setContentLength(contentLength);

            videoDownloadMetrics.incrementSuccessfulDownloads();
            videoDownloadMetrics.recordDownloadDuration(timer);
            videoDownloadMetrics.addDownloadedFileSize(contentLength);

            savedDownload = saveDownloadRecord(batch, downloadFileName);

            if (savedDownload != null) {
                videoStatusManagerService.updateEntityStatus(videoDownloadRepository, savedDownload.getId(), VideoStatusEnum.COMPLETED, "VideoDownloadService");
            } else {
                logger.warn("Registro de download não foi salvo, impossível atualizar status para COMPLETED.");
            }

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);

        } catch (ProcessedFileNotFoundException e) {
            logger.error("Arquivo não encontrado para batchProcessId {}: {}", rawBatchProcessId, e.getMessage());
            if (savedDownload != null) {
                videoStatusManagerService.updateEntityStatus(videoDownloadRepository, savedDownload.getId(), VideoStatusEnum.ERROR, "ProcessedFileNotFoundException");
            } else {
                updateErrorStatus(rawBatchProcessId, "ProcessedFileNotFoundException");
            }
            videoDownloadMetrics.incrementFailedDownloads();
            videoDownloadMetrics.recordDownloadDuration(timer);
            throw e;

        } catch (Exception e) {
            logger.error("Erro inesperado no download para batchProcessId {}: {}", rawBatchProcessId, e.getMessage(), e);
            if (savedDownload != null) {
                videoStatusManagerService.updateEntityStatus(videoDownloadRepository, savedDownload.getId(), VideoStatusEnum.ERROR, "UnexpectedError");
            } else {
                updateErrorStatus(rawBatchProcessId, "UnexpectedError");
            }
            videoDownloadMetrics.incrementFailedDownloads();
            videoDownloadMetrics.recordDownloadDuration(timer);
            throw new RuntimeException("Erro interno ao processar o download do vídeo.", e);
        }
    }


    private String extractS3Key(String filePath) {
        try {
            if (filePath == null || filePath.isBlank()) {
                throw new ProcessedFileNotFoundException("URL do S3 vazia ou nula.");
            }

            String cleanUrl = filePath.split("\\?")[0];

            URI uri = URI.create(cleanUrl);
            String path = uri.getPath();
            String key = path.startsWith("/") ? path.substring(1) : path;

            if (key.startsWith(bucketName + "/")) {
                key = key.substring(bucketName.length() + 1);
            }

            if (cleanUrl.contains(".s3.amazonaws.com/")) {
                key = cleanUrl.substring(cleanUrl.indexOf(".s3.amazonaws.com/") + ".s3.amazonaws.com/".length());
            } else if (cleanUrl.contains(bucketName + ".s3." + Region.US_EAST_1.id() + ".amazonaws.com/")) {
                key = cleanUrl.substring(
                        cleanUrl.indexOf(bucketName + ".s3." + Region.US_EAST_1.id() + ".amazonaws.com/") +
                                (bucketName + ".s3." + Region.US_EAST_1.id() + ".amazonaws.com/").length());
            }

            return key.startsWith("/") ? key.substring(1) : key;

        } catch (Exception e) {
            logger.error("Falha ao extrair a chave do S3 da URL: {}", filePath, e);
            throw new ProcessedFileNotFoundException("Erro ao processar a URL do S3.", e);
        }
    }

    private VideoDownload saveDownloadRecord(VideoProcessingBatch batch, String fileName) {
        try {
            if (batch == null || fileName == null || fileName.isBlank()) return null;

            VideoDownload download = new VideoDownload();
            download.setVideoProcessingBatch(batch);
            download.setDownloadFileName(fileName);
            download.setCreatedTimes(ZonedDateTime.now());
            download.setUpdatedTimes(ZonedDateTime.now());
            download.setStatus(VideoStatusEnum.PROCESSING);
            download.setS3Url(batch.getS3Url());
            download.setRetryCount(0);
            download.setUserAccount(batch.getUserAccount());

            VideoDownload savedDownload = videoDownloadRepository.save(download);
            logger.info("Download registrado com sucesso: {}", savedDownload.getId());

            videoDownloadProducer.sendDownloadId(savedDownload.getId());

            return savedDownload; 
        } catch (Exception e) {
            logger.error("Erro ao registrar download no banco: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao registrar download.", e);
        }
    }

    private void updateErrorStatus(String batchId, String source) {
        try {
            VideoProcessingBatch batch = videoProcessingBatchFinderService.findById(batchId);
            videoStatusManagerService.updateEntityStatus(videoDownloadRepository, batch.getId(), VideoStatusEnum.ERROR, source);
        } catch (Exception e) {
            logger.error("Erro ao atualizar status de erro para batchId {}: {}", batchId, e.getMessage(), e);
        }
    }

    private String detectMimeType(String filename) {
        if (filename.endsWith(".mp4")) return "video/mp4";
        if (filename.endsWith(".avi")) return "video/avi";
        if (filename.endsWith(".mov")) return "video/quicktime";
        return "application/octet-stream";
    }
}