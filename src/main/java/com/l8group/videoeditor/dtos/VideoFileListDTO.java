package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;
import com.l8group.videoeditor.enums.VideoStatusEnum;

public class VideoFileListDTO {

    private String fileName;
    private ZonedDateTime createdAt;
    private VideoStatusEnum status;

    public VideoFileListDTO(String fileName, ZonedDateTime createdAt, VideoStatusEnum status) {
        this.fileName = fileName;
        this.createdAt = createdAt;
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public VideoStatusEnum getStatus() {
        return status;
    }

    public void setStatus(VideoStatusEnum status) {
        this.status = status;
    }
}
