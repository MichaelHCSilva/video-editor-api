package com.l8group.videoeditor.dtos;

import java.util.UUID;

import com.l8group.videoeditor.enums.OverlayPosition;

public class VideoOverlayResponseDTO {

    private UUID id;
    private String overlayData; 
    private OverlayPosition position; 
    private Integer fontSize; 

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOverlayData() {
        return overlayData;
    }

    public void setOverlayData(String overlayData) {
        this.overlayData = overlayData;
    }

    public OverlayPosition getPosition() {
        return position;
    }

    public void setPosition(OverlayPosition position) {
        this.position = position;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    
}
