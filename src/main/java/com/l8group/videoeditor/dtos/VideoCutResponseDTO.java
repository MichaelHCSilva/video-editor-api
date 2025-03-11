package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

public class VideoCutResponseDTO {

    
    private String duration;
    private ZonedDateTime createdAt;

    public VideoCutResponseDTO( String duration, ZonedDateTime createdAt) {
        
        this.duration = duration;
        this.createdAt = createdAt;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    
}
