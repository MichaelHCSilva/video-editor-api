package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class VideoResizeRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório")
    private String videoId;

    @Positive(message = "A largura informada não atende aos requisitos. Certifique-se de que é um valor positivo e compatível com as resoluções suportadas.")
    private int width;

    @Positive(message = "A altura informada não atende aos requisitos. Certifique-se de que é um valor positivo e compatível com as resoluções suportadas.")
    private int height;

    public VideoResizeRequest() {
    }

    public VideoResizeRequest(String videoId, int width, int height) {
        this.videoId = videoId;
        this.width = width;
        this.height = height;
    }

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
}
