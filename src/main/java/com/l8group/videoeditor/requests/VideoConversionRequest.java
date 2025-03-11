package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VideoConversionRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório.")
    private String videoId;

    @NotBlank(message = "O formato de saída é obrigatório.")
    @Pattern(regexp = "^(mp4|avi|mov)$", message = "Formato de saída inválido. Formatos suportados: mp4, avi, mov.")
    private String outputFormat;

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
}