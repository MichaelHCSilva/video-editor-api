package com.l8group.videoeditor.dtos;

import com.l8group.videoeditor.enums.SupportedVideoResolution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class VideoResizeDTO {

    @NotBlank(message = "O ID do vídeo é obrigatório.")
    private String videoId;

    @Positive(message = "A largura deve ser maior que zero.")
    private int width;

    @Positive(message = "A altura deve ser maior que zero.")
    private int height;

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public SupportedVideoResolution getResolution() {
        return SupportedVideoResolution.fromDimensions(width, height);
    }
}
