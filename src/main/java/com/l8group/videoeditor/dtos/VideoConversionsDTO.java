package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

public class VideoConversionsDTO {

    private String fileName;
    private String fileFormat;
    private String targetFormat;
    private ZonedDateTime createdAt;

    public VideoConversionsDTO(String fileName, String fileFormat, String targetFormat, ZonedDateTime createdAt){
        this.fileName = fileName; 
        this.fileFormat = fileFormat;
        this.targetFormat = targetFormat;
        this.createdAt = createdAt;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    
   

}
