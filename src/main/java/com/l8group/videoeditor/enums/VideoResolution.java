package com.l8group.videoeditor.enums;

public enum VideoResolution {
    RES_1280x720(1280, 720),
    RES_1920x1080(1920, 1080),
    RES_720x1280(720, 1280),
    RES_1080x1920(1080, 1920),
    RES_600x600(600, 600),
    RES_720x720(720, 720),
    RES_1080x1080(1080, 1080);

    private final int width;
    private final int height;

    VideoResolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getResolutionAsString() {
        return width + "x" + height;
    }

    public static VideoResolution fromDimensions(int width, int height) {
        for (VideoResolution res : values()) {
            if (res.width == width && res.height == height) {
                return res;
            }
        }
        throw new IllegalArgumentException("Resolução não suportada: " + width + "x" + height);
    }
}
