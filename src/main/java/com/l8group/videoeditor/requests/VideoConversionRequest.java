package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VideoConversionRequest {

    @NotBlank(message = "O ID do vídeo é obrigatorio.")
    private String videoId;

    @NotBlank(message = "O formato do vídeo é obrigatorio")
    @Pattern(regexp = "^(mp4|avi|mov)$", message = "Formato inválido. Formatos suportados: mp4, avi, mov.")
    private String format;

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }


}
