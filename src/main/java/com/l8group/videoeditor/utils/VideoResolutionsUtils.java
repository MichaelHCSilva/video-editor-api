package com.l8group.videoeditor.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VideoResolutionsUtils {
    private static final List<String> VALID_RESOLUTIONS = Arrays.asList(
            "1280x720", "1920x1080", "720x1280", "1080x1920",
            "600x600", "720x720", "1080x1080");

    public static List<String> getValidResolutions() {
        return VALID_RESOLUTIONS;
    }

    public static boolean isValidResolution(int width, int height) {
        return VALID_RESOLUTIONS.contains(width + "x" + height);
    }

    public static String getSupportedResolutionsAsString() {
        return VALID_RESOLUTIONS.stream().collect(Collectors.joining(", "));
    }
}