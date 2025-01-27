package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

public class VideoCutResponseDTO {

    private String cutFileName;
    private String duration;
    private ZonedDateTime uploadedAt;

    public VideoCutResponseDTO(String cutFileName, String duration, ZonedDateTime uploadedAt) {
        this.cutFileName = cutFileName;
        this.duration = duration;
        this.uploadedAt = uploadedAt;
    }

    public String getCutFileName() {
        return cutFileName;
    }

    public void setCutFileName(String cutFileName) {
        this.cutFileName = cutFileName;
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
