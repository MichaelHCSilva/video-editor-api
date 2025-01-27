package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

import com.l8group.videoeditor.enums.VideoStatus;

public class VideoFileResponseDTO {

    private String fileName;
    private ZonedDateTime uploadedAt;
    private VideoStatus status;
    
    public VideoFileResponseDTO(String fileName, ZonedDateTime uploadedAt, VideoStatus status) {
        this.fileName = fileName;
        this.uploadedAt = uploadedAt;
        this.status = status;
        
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


}
