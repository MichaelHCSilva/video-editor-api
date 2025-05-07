package com.l8group.videoeditor.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class VideoS3Service {

    private static final Logger logger = LoggerFactory.getLogger(VideoS3Service.class);

    private final S3Client s3Client;
    private final Region region;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public static final String RAW_VIDEO_FOLDER = "raw-videos/";
    public static final String PROCESSED_VIDEO_FOLDER = "processed-videos/";

    public VideoS3Service() {
        this.region = Region.US_EAST_1;
        this.s3Client = S3Client.builder()
                .region(this.region)
                .credentialsProvider(ProfileCredentialsProvider.create("editor-video-s3"))
                .build();
        logger.info("VideoS3Service inicializado para o bucket: {}, região: {}", bucketName, region);
    }

    @PostConstruct
    public void checkS3Client() {
        logger.info("Verificando S3 Client: bucket={}, região={}", bucketName, region);
    }

    public String uploadRawFile(File file, String fileName, UUID videoId) throws IOException {
        logger.info("Iniciando upload de arquivo raw: {}, nome original: {}, videoId: {}", file.getAbsolutePath(),
                fileName, videoId);
        String s3Key = generateUniqueFileName(fileName, videoId);
        logger.info("Nome do arquivo S3 gerado: {}", s3Key);
        return uploadToS3(file, s3Key, RAW_VIDEO_FOLDER);
    }

    public String uploadProcessedFile(File file, String fileName, UUID videoId) throws IOException {
        logger.info("Iniciando upload de arquivo processado: {}, nome original: {}, videoId: {}",
                file.getAbsolutePath(), fileName, videoId);
        String s3Key = fileName;
        logger.info("Nome do arquivo S3 para processado: {}", s3Key);
        return uploadToS3(file, s3Key, PROCESSED_VIDEO_FOLDER);
    }

    private String uploadToS3(File file, String s3Key, String folder) throws IOException {
        String fullKey = folder + s3Key;
        logger.info("Iniciando upload para S3: bucket={}, key={}", bucketName, fullKey);

        if (fileExists(fullKey)) {
            logger.warn("Arquivo já existe no S3: {}", fullKey);
            throw new IOException("Arquivo já existe no S3: " + fullKey);
        }

        String contentType = getContentType(s3Key);
        logger.info("Content type detectado para {}: {}", s3Key, contentType);

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .contentType(contentType)
                    .contentDisposition("inline")
                    .build();
            logger.debug("PutObjectRequest: {}", putObjectRequest);

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(fileInputStream, file.length()));

            logger.info("Upload para S3 concluído: bucket={}, key={}", bucketName, fullKey);
            String fileUrl = getFileUrl(folder, s3Key);
            logger.info("URL do arquivo S3 gerado: {}", fileUrl);
            return fileUrl;
        } catch (S3Exception e) {
            logger.error("Erro no upload S3 (key={}): {}", fullKey, e.awsErrorDetails().errorMessage());
            throw new IOException("Falha ao fazer upload do arquivo para o S3.", e);
        }
    }

    public String getFileUrl(String folder, String fileName) {
        String fullKey = folder + fileName;
        logger.info("Obtendo URL do S3 para: bucket={}, key={}", bucketName, fullKey);
        try {
            GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .build();
            logger.debug("GetUrlRequest: {}", getUrlRequest);
            String url = s3Client.utilities().getUrl(getUrlRequest).toString();
            logger.info("URL do S3 obtido: {}", url);
            return url;
        } catch (S3Exception e) {
            logger.error("Erro ao obter URL do S3 para key {}: {}", fullKey, e.awsErrorDetails().errorMessage());
            return null;
        }
    }

    public void deleteFile(String fileName, boolean isProcessed) {
        String folder = isProcessed ? PROCESSED_VIDEO_FOLDER : RAW_VIDEO_FOLDER;
        String fullKey = folder + fileName;
        logger.info("Iniciando exclusão de arquivo do S3: bucket={}, key={}", bucketName, fullKey);
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .build();
            logger.debug("DeleteObjectRequest: {}", deleteObjectRequest);
            s3Client.deleteObject(deleteObjectRequest);
            logger.info("Arquivo removido do S3: {}", fullKey);
        } catch (S3Exception e) {
            logger.error("Erro ao excluir arquivo {} do S3: {}", fullKey, e.awsErrorDetails().errorMessage());
        }
    }

    public boolean fileExists(String fullKey) {
        logger.info("Verificando se arquivo existe no S3: bucket={}, key={}", bucketName, fullKey);
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .build();
            logger.debug("HeadObjectRequest: {}", headObjectRequest);
            s3Client.headObject(headObjectRequest);
            logger.debug("Arquivo {} encontrado no S3.", fullKey);
            return true;
        } catch (NoSuchKeyException e) {
            logger.debug("Arquivo {} não encontrado no S3.", fullKey);
            return false;
        } catch (S3Exception e) {
            logger.error("Erro ao verificar existência do arquivo S3 (key={}): {}", fullKey,
                    e.awsErrorDetails().errorMessage());
            return false;
        }
    }

    public void downloadFile(String s3Url, String localFilePath) throws IOException {
        logger.info("Iniciando download do S3 para local: url={}, destino={}", s3Url, localFilePath);
        try {
            URI uri = URI.create(s3Url);
            String s3Key = uri.getPath().startsWith("/") ? uri.getPath().substring(1) : uri.getPath();
            logger.info("Chave S3 extraída da URL: {}", s3Key);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            logger.debug("GetObjectRequest para download: {}", getObjectRequest);

            s3Client.getObject(
                    getObjectRequest,
                    ResponseTransformer.toFile(Paths.get(localFilePath)));
            logger.info("Download concluído do S3 para: {}", localFilePath);
        } catch (S3Exception e) {
            logger.error("Erro ao baixar do S3 (url={}): {}", s3Url, e.awsErrorDetails().errorMessage());
            throw new IOException("Erro ao baixar o arquivo do S3.", e);
        }
    }

    private String generateUniqueFileName(String originalFileName, UUID videoId) {
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        String uniqueFileName = videoId + extension.toLowerCase(Locale.ROOT);
        logger.debug("Nome de arquivo único gerado: original={}, videoId={}, resultado={}", originalFileName, videoId,
                uniqueFileName);
        return uniqueFileName;
    }

    private String getContentType(String fileName) {
        String ext = fileName.toLowerCase(Locale.ROOT);
        String contentType = "application/octet-stream";
        if (ext.endsWith(".mp4"))
            contentType = "video/mp4";
        if (ext.endsWith(".avi"))
            contentType = "video/x-msvideo";
        if (ext.endsWith(".mov"))
            contentType = "video/quicktime";
        logger.debug("Content type para {}: {}", fileName, contentType);
        return contentType;
    }
}