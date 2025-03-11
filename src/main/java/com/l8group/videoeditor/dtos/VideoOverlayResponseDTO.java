package com.l8group.videoeditor.dtos;

import com.l8group.videoeditor.enums.OverlayPosition;

import java.util.UUID;

public class VideoOverlayResponseDTO {

    private UUID id;
    private String watermark;
    private OverlayPosition position;
    private Integer fontSize;

    public VideoOverlayResponseDTO(UUID id, String watermark, OverlayPosition position, Integer fontSize) {
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

    public OverlayPosition getPosition() {
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

    public void setPosition(OverlayPosition position) {
        this.position = position;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }
}