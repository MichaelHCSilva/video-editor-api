package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

public class VideoCutResponseDTO {

    private String fileName;
    private String duration;
    private ZonedDateTime uploadedAt;

    public VideoCutResponseDTO(String fileName, String duration, ZonedDateTime uploadedAt) {
        this.fileName = fileName;
        this.duration = duration;
        this.uploadedAt = uploadedAt;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public ZonedDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(ZonedDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
