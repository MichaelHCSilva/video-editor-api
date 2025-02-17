package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

import com.l8group.videoeditor.enums.VideoStatus;

public class VideoFileResponseDTO {

    private final String fileName;
    private final ZonedDateTime createdAt;
    private final VideoStatus status;

    public VideoFileResponseDTO(String fileName, ZonedDateTime createdAt, VideoStatus status) {
        this.fileName = fileName;
        this.createdAt = createdAt;
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public VideoStatus getStatus() {
        return status;
    }
}
