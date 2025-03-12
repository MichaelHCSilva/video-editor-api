package com.l8group.videoeditor.dtos;

import com.l8group.videoeditor.enums.OverlayPositionEnum;

import java.util.UUID;

public class VideoOverlayResponseDTO {

    private UUID id;
    private String watermark;
    private OverlayPositionEnum position;
    private Integer fontSize;

    public VideoOverlayResponseDTO(UUID id, String watermark, OverlayPositionEnum position, Integer fontSize) {
        this.id = id;
        this.watermark = watermark;
        this.position = position;
        this.fontSize = fontSize;
    }

    public UUID getId() {
        return id;
    }

    public String getWatermark() {
        return watermark;
    }

    public OverlayPositionEnum getPosition() {
        return position;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    // (Opcional) Setters
    public void setId(UUID id) {
        this.id = id;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public void setPosition(OverlayPositionEnum position) {
        this.position = position;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }
}