package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

public class VideoConversionsDTO {

    private String inputFormat;
    private String outputFormat;
    private ZonedDateTime createdAt;

    public VideoConversionsDTO(String inputFormat, String outputFormat, ZonedDateTime createdAt) {
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.createdAt = createdAt;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
}