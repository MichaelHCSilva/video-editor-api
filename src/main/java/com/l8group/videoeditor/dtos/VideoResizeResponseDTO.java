package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

public class VideoResizeResponseDTO {
    
    private String resolution;
    private ZonedDateTime createdAt;

    public VideoResizeResponseDTO( String resolution, ZonedDateTime createdAt) {
        this.resolution = resolution;
        this.createdAt = createdAt;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }


}
