package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

import com.l8group.videoeditor.models.VideoConversion;

public class VideoConversionsDTO {

    private String originalFilename;
    private String originalFormat;
    private String targetFormat;
    private ZonedDateTime createdAt;

    public VideoConversionsDTO(VideoConversion videoConversion) {

        this.originalFilename = videoConversion.getVideoFile().getFileName();
        this.originalFormat = videoConversion.getFileFormat();
        this.targetFormat = videoConversion.getTargetFormat();
        this.createdAt = videoConversion.getCreatedAt();
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getOriginalFormat() {
        return originalFormat;
    }

    public void setOriginalFormat(String originalFormat) {
        this.originalFormat = originalFormat;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

   

}
