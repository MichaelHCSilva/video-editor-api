package com.l8group.videoeditor.enums;

public enum OverlayPosition {
    TOP_LEFT("top-left"),
    TOP_RIGHT("top-right"),
    BOTTOM_LEFT("bottom-left"),
    BOTTOM_RIGHT("bottom-right"),
    CENTER("center");

    private final String position;

    OverlayPosition(String position) {
        this.position = position;
    }

    public String getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return position;
    }
}

