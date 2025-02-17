package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class VideoBatchResponseDTO {
    private UUID videoId;
    private String fileName;
    private ZonedDateTime createdAt;
    private List<String> operations;

    public VideoBatchResponseDTO(UUID videoId,String fileName, ZonedDateTime createdAt, List<String> operations) {
        this.videoId = videoId;
        this.fileName = fileName;
        this.createdAt = createdAt;
        this.operations = operations;

    }

    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
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

    public List<String> getOperations() {
        return operations;
    }

    public void setOperations(List<String> operations) {
        this.operations = operations;
    }

    

}
