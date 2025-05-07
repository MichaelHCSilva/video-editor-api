package com.l8group.videoeditor.utils;

import java.util.Arrays;
import java.util.List;

public class VideoOverlayPositionUtils {

    public static final String TOP_LEFT = "top-left";
    public static final String TOP_RIGHT = "top-right";
    public static final String BOTTOM_LEFT = "bottom-left";
    public static final String BOTTOM_RIGHT = "bottom-right";
    public static final String CENTER = "center";

    private static final List<String> VALID_POSITIONS = Arrays.asList(
            TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER);

    private VideoOverlayPositionUtils() {
    }

    public static boolean isValidPosition(String position) {
        return VALID_POSITIONS.contains(position);
    }

    public static List<String> getAllPositions() {
        return VALID_POSITIONS;
    }
}