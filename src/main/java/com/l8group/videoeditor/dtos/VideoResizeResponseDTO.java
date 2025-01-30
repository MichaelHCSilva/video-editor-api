package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

public class VideoResizeResponseDTO {
    private String fileName;
    private String resolution;
    private ZonedDateTime date;
    private String message;

    public VideoResizeResponseDTO(String fileName, String resolution, ZonedDateTime date) {
        this.fileName = fileName;
        this.resolution = resolution;
        this.date = date;
        this.message = "O vídeo foi redimensionado com sucesso.";
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
