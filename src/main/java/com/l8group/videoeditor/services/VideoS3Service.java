package com.l8group.videoeditor.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.s3.S3SignedUrlService;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class VideoS3Service {

    private static final Logger logger = LoggerFactory.getLogger(VideoS3Service.class);

    private final S3Client s3Client;
    private final S3SignedUrlService s3SignedUrlService;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

     @Value("${aws.s3.signed-url.expiration-minutes}")
    private long signedUrlExpirationMinutes;

    public static final String RAW_VIDEO_FOLDER = "raw-videos/";
    public static final String PROCESSED_VIDEO_FOLDER = "processed-videos/";

    public VideoS3Service(S3SignedUrlService s3SignedUrlService) {
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.builder()
                .profileName("editor-video-s3")
                .build())
            .build();
        this.s3SignedUrlService = s3SignedUrlService;
    }

    public String uploadRawFile(File file, String fileName, UUID videoId) throws IOException {
        logger.info("Iniciando o upload do arquivo raw para o S3. Vídeo ID: {}, Nome do Arquivo: {}", videoId, fileName);
        String s3Key = generateVersionedFileName(fileName, RAW_VIDEO_FOLDER);
        logger.info("Nome único gerado para o arquivo raw: {}", s3Key);
        return uploadToS3(file, s3Key, RAW_VIDEO_FOLDER);
    }

    public String uploadProcessedFile(File file, String fileName, UUID videoId) throws IOException {
        logger.info("Iniciando o upload do arquivo processado para o S3. Vídeo ID: {}, Nome do Arquivo: {}", videoId, fileName);
        String s3Key = generateVersionedFileName(fileName, PROCESSED_VIDEO_FOLDER);
        logger.info("Nome único gerado para o arquivo processado: {}", s3Key);
        return uploadToS3(file, s3Key, PROCESSED_VIDEO_FOLDER);
    }

    private String generateVersionedFileName(String baseFileName, String folder) {
        String nameWithoutExtension = FilenameUtils.removeExtension(baseFileName);
        String extension = FilenameUtils.getExtension(baseFileName);

        String versionedFileName = baseFileName;
        int version = 1;

        while (fileExists(folder + versionedFileName)) {
            versionedFileName = String.format("%s_v%d.%s", nameWithoutExtension, version, extension);
            version++;
        }

        return versionedFileName;
    }

    private boolean fileExists(String fullKey) {
        try {
            logger.debug("Verificando existência do arquivo no S3: {}", fullKey);
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    private String uploadToS3(File file, String s3Key, String folder) throws IOException {
        String fullKey = folder + s3Key;
        String contentType = getContentType(s3Key);
        logger.info("Upload para S3 - Arquivo: {}, Content-Type: {}", fullKey, contentType);

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .contentType(contentType)
                    .contentDisposition("inline")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(fileInputStream, file.length()));
            logger.info("Upload realizado com sucesso para: {}", fullKey);



             return s3SignedUrlService.generateSignedUrl(bucketName, fullKey, Duration.ofMinutes(signedUrlExpirationMinutes));
        } catch (S3Exception e) {
            logger.error("Erro no upload para o S3. Arquivo: {}", fullKey, e);
            throw new IOException("Falha ao enviar o arquivo para o S3.", e);
        }
    }

   /* private String getFileUrl(String folder, String fileName) {
        String fullKey = folder + fileName;
        GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                .bucket(bucketName)
                .key(fullKey)
                .build();
        return s3Client.utilities().getUrl(getUrlRequest).toString();
    }*/

    private String getContentType(String filename) {
        if (filename.endsWith(".mp4")) return "video/mp4";
        if (filename.endsWith(".avi")) return "video/avi";
        if (filename.endsWith(".mov")) return "video/quicktime";
        return "application/octet-stream";
    }
}
