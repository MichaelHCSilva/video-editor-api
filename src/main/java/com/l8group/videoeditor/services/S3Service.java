package com.l8group.videoeditor.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public static final String RAW_VIDEO_FOLDER = "raw-videos/";
    public static final String PROCESSED_VIDEO_FOLDER = "processed-videos/";

    public S3Service(@Value("${aws.s3.region}") String region) {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(ProfileCredentialsProvider.create("editor-video-s3"))
                .build();
    }

    public String uploadRawFile(File file, String fileName, UUID videoId) throws IOException {
        return uploadToS3(file, fileName, RAW_VIDEO_FOLDER);
    }

    public String uploadProcessedFile(File file, String fileName, UUID videoId) throws IOException {
        return uploadToS3(file, fileName, PROCESSED_VIDEO_FOLDER);
    }

    private String uploadToS3(File file, String fileName, String folder) throws IOException {
        String newFileName = generateFileName(fileName);
        String s3Key = folder + newFileName;

        try {
            if (fileExists(s3Key)) {
                logger.warn("Arquivo já existe no S3: {}", s3Key);
                throw new IOException("Arquivo já existe no S3: " + s3Key);
            }

            String contentType = getContentType(fileName);

            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(contentType)
                        .contentDisposition("inline")
                        .build(),
                        RequestBody.fromInputStream(fileInputStream, file.length()));

                logger.info("Arquivo enviado para o S3 na pasta '{}' com o nome: {}", folder, s3Key);
            }
        } catch (S3Exception e) {
            logger.error("Erro ao enviar arquivo para o S3: {}", e.getMessage());
            throw new IOException("Falha ao fazer upload do arquivo para o S3.", e);
        }

        return getFileUrl(folder, newFileName);
    }

    public String getFileUrl(String folder, String fileName) {
        try {
            String fullFileName = folder + fileName;
            return s3Client.utilities().getUrl(GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(fullFileName)
                    .build()).toString();
        } catch (S3Exception e) {
            logger.error("Erro ao obter URL do arquivo do S3: {}", e.getMessage());
            return null;
        }
    }

    public void deleteFile(String fileName, boolean isProcessed) {
        try {
            String folder = isProcessed ? PROCESSED_VIDEO_FOLDER : RAW_VIDEO_FOLDER;
            String fullFileName = folder + fileName;
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullFileName)
                    .build());
            logger.info("Arquivo excluído do S3: {}", fullFileName);
        } catch (S3Exception e) {
            logger.error("Erro ao excluir arquivo do S3: {}", e.getMessage());
        }
    }

    private String generateFileName(String originalFileName) {
        return originalFileName;
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (fileName.endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (fileName.endsWith(".mov")) {
            return "video/quicktime";
        } else {
            return "application/octet-stream";
        }
    }

    private boolean fileExists(String fileName) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            logger.error("Erro ao verificar a existência do arquivo: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Baixa o arquivo do S3 e salva no local especificado.
     * 
     * @param s3Url         URL S3 do arquivo a ser baixado.
     * @param localFilePath Caminho local onde o arquivo será salvo.
     * @throws IOException Se ocorrer um erro durante o download.
     */
    public void downloadFile(String s3Url, String localFilePath) throws IOException {
        try {
            // O S3 usa o URL do arquivo (o caminho armazenado no videoFilePath) para baixar
            // o arquivo
            s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Url) // Usando o caminho do arquivo no S3
                    .build(),
                    ResponseTransformer.toFile(Paths.get(localFilePath))); // Baixando para o localFilePath

            logger.info("Arquivo baixado com sucesso do S3: {}", s3Url);
        } catch (S3Exception e) {
            logger.error("Erro ao baixar o arquivo do S3: {}", e.getMessage());
            throw new IOException("Falha ao baixar o arquivo do S3", e);
        }
    }

}