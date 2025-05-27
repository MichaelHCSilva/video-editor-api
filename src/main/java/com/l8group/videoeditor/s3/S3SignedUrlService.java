package com.l8group.videoeditor.s3;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

@Service
public class S3SignedUrlService {

    private S3Presigner presigner;

    @PostConstruct
    public void init() {
        this.presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        ProfileCredentialsProvider.builder()
                                .profileName("editor-video-s3")
                                .build()
                )
                .build();
    }

    public String generateSignedUrl(String bucketName, String objectKey, Duration expiration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(getObjectRequest)
                .build();

        URL signedUrl = presigner.presignGetObject(presignRequest).url();
        return signedUrl.toString();
    }

    @PreDestroy
    public void shutdown() {
        if (presigner != null) {
            presigner.close();
        }
    }
}
