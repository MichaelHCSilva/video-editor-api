package com.l8group.videoeditor.services;

import com.l8group.videoeditor.exceptions.ProcessedFileNotFoundException;
import com.l8group.videoeditor.models.VideoProcessingBatch;
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

import java.io.InputStream;
import java.net.URI;

@Service
public class VideoDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadService.class);

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private final S3Client s3Client;
    private final VideoProcessingBatchFinderService videoProcessingBatchFinderService;
    private final VideoDownloadValidator requestValidator;

    public VideoDownloadService(VideoProcessingBatchFinderService finderService,
            VideoDownloadValidator requestValidator) {
        logger.info("Inicializando VideoDownloadService...");
        this.videoProcessingBatchFinderService = finderService;
        this.requestValidator = requestValidator;
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create("editor-video-s3"))
                .build();
        logger.info("S3 Client configurado para a região: {}", Region.US_EAST_1.id());
        logger.info("VideoDownloadService inicializado.");
    }

    public ResponseEntity<InputStreamResource> downloadVideoStreamFromS3(String rawBatchProcessId) {
        logger.info("Iniciando downloadVideoStreamFromS3 para batchProcessId: {}", rawBatchProcessId);
        requestValidator.validateRawBatchProcessId(rawBatchProcessId);

        VideoProcessingBatch batch = videoProcessingBatchFinderService.findById(rawBatchProcessId);
        requestValidator.validateVideoProcessingBatch(batch);

        String filePath = batch.getVideoFilePath();
        logger.info("Caminho do arquivo obtido do processamento em lote: {}", filePath);

        String downloadFileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        logger.info("Nome do arquivo para download: {}", downloadFileName);

        try {
            InputStreamResource resource = fetchVideoFromS3(filePath);
            logger.info("Arquivo de vídeo do S3 retornado com sucesso para batchProcessId: {}", rawBatchProcessId);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadFileName + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, detectMimeType(downloadFileName));

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Erro ao acessar o arquivo no S3 para o caminho: {}. Erro: {}", filePath, e.getMessage());
            throw new ProcessedFileNotFoundException("Erro ao acessar o arquivo no S3.");
        }
    }

    private InputStreamResource fetchVideoFromS3(String filePath) {
        logger.info("Iniciando fetchVideoFromS3 para o caminho: {}", filePath);
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
                    logger.debug("Chave ajustada removendo o nome do bucket: {}", key);
                } else if (filePath.contains(".s3.amazonaws.com/")) {
                    int index = filePath.indexOf(".s3.amazonaws.com/");
                    if (index != -1) {
                        key = filePath.substring(index + ".s3.amazonaws.com/".length());
                        if (key.startsWith("/")) {
                            key = key.substring(1);
                        }
                        logger.debug("Chave ajustada removendo o domínio padrão do S3: {}", key);
                    }
                } else if (filePath.contains(bucketName + ".s3." + Region.US_EAST_1.id() + ".amazonaws.com/")) {
                    int index = filePath.indexOf(bucketName + ".s3." + Region.US_EAST_1.id() + ".amazonaws.com/");
                    if (index != -1) {
                        key = filePath.substring(
                                index + (bucketName + ".s3." + Region.US_EAST_1.id() + ".amazonaws.com/").length());
                        if (key.startsWith("/")) {
                            key = key.substring(1);
                        }
                        logger.debug("Chave ajustada removendo o domínio regional do S3: {}", key);
                    }
                }
                logger.info("Chave S3 extraída do URL: {}", key);

            } catch (Exception e) {
                logger.error("Erro ao processar o URL do S3: {}", e.getMessage());

            }
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        logger.debug("GetObjectRequest construído: {}", getObjectRequest);

        ResponseInputStream<GetObjectResponse> responseInputStream = null;
        try {
            logger.info("Fazendo requisição para obter objeto do S3: bucket={}, key={}", bucketName, key);
            responseInputStream = s3Client.getObject(getObjectRequest);
            logger.info("Objeto do S3 obtido com sucesso.");
        } catch (Exception e) {
            logger.error("Erro ao obter objeto do S3 (bucket={}, key={}): {}", bucketName, key, e.getMessage());
            throw new ProcessedFileNotFoundException("Erro ao acessar o arquivo no S3.");
        }

        InputStream inputStream = responseInputStream != null ? responseInputStream : null;
        logger.debug("InputStream obtido: {}", inputStream);

        if (inputStream == null) {
            logger.error("InputStream nulo retornado do S3 para o caminho: {}", filePath);
            throw new ProcessedFileNotFoundException("Arquivo não encontrado no S3.");
        }

        InputStreamResource resource = new InputStreamResource(inputStream);
        logger.info("InputStreamResource criado e retornado para o caminho: {}", filePath);
        return resource;
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

}