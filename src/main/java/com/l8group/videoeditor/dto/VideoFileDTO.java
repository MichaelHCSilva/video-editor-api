package com.l8group.videoeditor.dto;

import java.time.ZonedDateTime;

import com.l8group.videoeditor.enums.VideoStatus;

public class VideoFileDTO {
    private final String fileName;
    private final ZonedDateTime uploadedAt;
    private final VideoStatus status;
    private final Long duration;

    public VideoFileDTO(String fileName, ZonedDateTime uploadedAt, VideoStatus status, Long duration) {
        this.fileName = fileName;
        this.uploadedAt = uploadedAt;
        this.status = status;
        this.duration = duration;
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

    public Long getDuration(){
        return duration;
    }
}
