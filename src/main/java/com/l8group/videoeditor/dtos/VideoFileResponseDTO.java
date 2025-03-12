package com.l8group.videoeditor.dtos;

//import com.l8group.videoeditor.enums.VideoStatus;
import java.time.ZonedDateTime;
import java.util.UUID;

public class VideoFileResponseDTO {
    private UUID id; 
    private String fileName;
    private ZonedDateTime createdAt;
    
    public VideoFileResponseDTO(UUID id, String fileName, ZonedDateTime createdAt) {
        this.id = id;
        this.fileName = fileName;
        this.createdAt = createdAt;
        
    }

    public UUID getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

}
