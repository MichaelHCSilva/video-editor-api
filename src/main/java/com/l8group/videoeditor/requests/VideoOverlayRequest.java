package com.l8group.videoeditor.requests;

import com.l8group.videoeditor.enums.OverlayPositionEnum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VideoOverlayRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório.")
    private String videoId;

    @NotBlank(message = "Os dados de sobreposição são obrigatórios.")
    @Size(max = 255, message = "Os dados de sobreposição devem ter no máximo 255 caracteres.")
    private String watermark;

    @NotNull(message = "A posição é obrigatória.")
    private OverlayPositionEnum position;

    @Min(value = 1, message = "O tamanho da fonte deve ser maior que 0.")
    private Integer fontSize;

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public OverlayPositionEnum getPosition() {
        return position;
    }

    public void setPosition(OverlayPositionEnum position) {
        this.position = position;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

}
