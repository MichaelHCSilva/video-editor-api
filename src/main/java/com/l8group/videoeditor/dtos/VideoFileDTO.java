package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

import com.l8group.videoeditor.enums.VideoStatus;

public class VideoFileDTO {

    private String fileName;
    private ZonedDateTime uploadedAt;
    private VideoStatus status;
    private Long duration;

    public VideoFileDTO(String fileName, ZonedDateTime uploadedAt, VideoStatus status, Long duration) {
        this.fileName = fileName;
        this.uploadedAt = uploadedAt;
        this.status = status;
        this.duration = duration;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public ZonedDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(ZonedDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public VideoStatus getStatus() {
        return status;
    }

    public void setStatus(VideoStatus status) {
        this.status = status;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

}
