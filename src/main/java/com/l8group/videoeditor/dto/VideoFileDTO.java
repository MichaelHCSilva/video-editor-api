package com.l8group.videoeditor.dto;

import java.time.ZonedDateTime;

import com.l8group.videoeditor.enums.VideoStatus;

public class VideoFileDTO {
    private final String fileName;
    private final ZonedDateTime uploadedAt;
    private final VideoStatus status;

    public VideoFileDTO(String fileName, ZonedDateTime uploadedAt, VideoStatus status) {
        this.fileName = fileName;
        this.uploadedAt = uploadedAt;
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public ZonedDateTime getUploadedAt() {
        return uploadedAt;
    }

    public VideoStatus getStatus() {
        return status;
    }
}
