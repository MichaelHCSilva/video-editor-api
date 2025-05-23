package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.ProcessedFileNotFoundException;
import com.l8group.videoeditor.metrics.VideoDownloadMetrics;
import com.l8group.videoeditor.models.VideoDownload;
import com.l8group.videoeditor.models.VideoProcessingBatch;
import com.l8group.videoeditor.rabbit.producer.VideoDownloadProducer;
import com.l8group.videoeditor.repositories.VideoDownloadRepository;
import com.l8group.videoeditor.validation.VideoDownloadValidator;

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
    private final VideoProcessingBatchFinderService videoProcessingBatchFinderService;
    private final VideoDownloadValidator requestValidator;
    private final VideoDownloadMetrics videoDownloadMetrics;
    private final VideoDownloadRepository videoDownloadRepository;
    private final VideoDownloadProducer videoDownloadProducer;

    public VideoDownloadService(VideoProcessingBatchFinderService finderService,
            VideoDownloadValidator requestValidator,
            VideoDownloadMetrics videoDownloadMetrics,
            VideoDownloadRepository videoDownloadRepository,
            VideoDownloadProducer videoDownloadProducer) {

        logger.info("Inicializando VideoDownloadService...");
        this.videoProcessingBatchFinderService = finderService;
        this.requestValidator = requestValidator;
        this.videoDownloadMetrics = videoDownloadMetrics;
        this.videoDownloadRepository = videoDownloadRepository;
        this.videoDownloadProducer = videoDownloadProducer;

        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create("editor-video-s3"))
                .build();
        logger.info("S3 Client configurado para a região: {}", Region.US_EAST_1.id());
        logger.info("VideoDownloadService inicializado.");
    }

    public ResponseEntity<InputStreamResource> downloadVideoStreamFromS3(String rawBatchProcessId) {
        logger.info("Iniciando downloadVideoStreamFromS3 para batchProcessId: {}", rawBatchProcessId);

        videoDownloadMetrics.incrementDownloadRequests();
        Timer.Sample timer = videoDownloadMetrics.startDownloadTimer();

        VideoProcessingBatch batch;
        try {
            requestValidator.validateRawBatchProcessId(rawBatchProcessId);

            batch = videoProcessingBatchFinderService.findById(rawBatchProcessId);
            requestValidator.validateVideoProcessingBatch(batch);

            String filePath = batch.getVideoFilePath();
            logger.info("Caminho do arquivo obtido do processamento em lote: {}", filePath);

            String downloadFileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            logger.info("Nome do arquivo para download: {}", downloadFileName);

            ResponseInputStream<GetObjectResponse> responseInputStream = fetchVideoFromS3Internal(filePath);
            long contentLength = responseInputStream.response().contentLength();
            InputStreamResource resource = new InputStreamResource(responseInputStream);

            logger.info("Arquivo de vídeo do S3 retornado com sucesso para batchProcessId: {}", rawBatchProcessId);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadFileName + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, detectMimeType(downloadFileName));
            headers.setContentLength(contentLength);

            videoDownloadMetrics.incrementSuccessfulDownloads();
            videoDownloadMetrics.recordDownloadDuration(timer);
            videoDownloadMetrics.addDownloadedFileSize(contentLength);

            saveDownloadRecord(batch, downloadFileName); 

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);

        } catch (ProcessedFileNotFoundException e) {
            videoDownloadMetrics.incrementFailedDownloads();
            videoDownloadMetrics.recordDownloadDuration(timer);
            logger.error("Erro ao acessar o arquivo no S3 para o caminho do lote {}. Erro: {}", rawBatchProcessId,
                    e.getMessage());
            throw e;
        } catch (Exception e) {
            videoDownloadMetrics.incrementFailedDownloads();
            videoDownloadMetrics.recordDownloadDuration(timer);
            logger.error("Erro inesperado durante o download do vídeo para o lote {}. Erro: {}", rawBatchProcessId,
                    e.getMessage(), e);
            throw new RuntimeException("Erro interno ao processar o download do vídeo.", e);
        }
    }

    private ResponseInputStream<GetObjectResponse> fetchVideoFromS3Internal(String filePath) {
        logger.info("Iniciando fetchVideoFromS3Internal para o caminho: {}", filePath);
        String key = filePath;

        if (filePath != null && filePath.startsWith("https://")) {
            try {
                URI uri = URI.create(filePath);
                String path = uri.getPath();
                if (path.startsWith("/")) {
                    key = path.substring(1);
                }
                if (key.startsWith(bucketName + "/")) {
                    key = key.substring(bucketName.length() + 1);
                } else if (filePath.contains(".s3.amazonaws.com/")) {
                    key = filePath.substring(filePath.indexOf(".s3.amazonaws.com/") + ".s3.amazonaws.com/".length());
                } else if (filePath.contains(bucketName + ".s3." + Region.US_EAST_1.id() + ".amazonaws.com/")) {
                    key = filePath
                            .substring(filePath.indexOf(bucketName + ".s3." + Region.US_EAST_1.id() + ".amazonaws.com/")
                                    + (bucketName + ".s3." + Region.US_EAST_1.id() + ".amazonaws.com/").length());
                }
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }

                logger.info("Chave S3 extraída do URL: {}", key);
            } catch (Exception e) {
                logger.error("Erro ao processar o URL do S3: {}", e.getMessage(), e);
                throw new ProcessedFileNotFoundException("Erro ao processar o URL do S3.", e);
            }
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try {
            logger.info("Fazendo requisição para obter objeto do S3: bucket={}, key={}", bucketName, key);
            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            logger.error("Erro ao obter objeto do S3 (bucket={}, key={}): {}", bucketName, key, e.getMessage(), e);
            throw new ProcessedFileNotFoundException("Erro ao acessar o arquivo no S3.", e);
        }
    }

    private String detectMimeType(String filename) {
        if (filename.endsWith(".mp4"))
            return "video/mp4";
        if (filename.endsWith(".avi"))
            return "video/avi";
        if (filename.endsWith(".mov"))
            return "video/quicktime";
        return "application/octet-stream";
    }

    private void saveDownloadRecord(VideoProcessingBatch batch, String downloadFileName) {
        try {
            if (batch == null || downloadFileName == null || downloadFileName.isBlank()) {
                logger.warn("Batch ou nome do arquivo de download inválido. Ignorando persistência.");
                return;
            }

            logger.info("Salvando registro de download no banco para batchProcessId: {}", batch.getId());

            VideoDownload download = new VideoDownload();
            download.setVideoProcessingBatch(batch);
            download.setDownloadFileName(downloadFileName);
            download.setCreatedTimes(ZonedDateTime.now());
            download.setUpdatedTimes(ZonedDateTime.now());
            download.setStatus(VideoStatusEnum.PROCESSING);
            download.setVideoFilePath(batch.getVideoFilePath());
            download.setRetryCount(0);
            download.setUserAccount(batch.getUserAccount());

            videoDownloadRepository.save(download);
            logger.info("Registro de download salvo com sucesso: {}", download.getId());

            videoDownloadProducer.sendDownloadId(download.getId());

        } catch (Exception e) {
            logger.error("Erro ao salvar registro de download no banco: {}", e.getMessage(), e);
        }
    }

}
