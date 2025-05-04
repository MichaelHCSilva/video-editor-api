package com.l8group.videoeditor.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class VideoFileNameGenerator {

    private static final String DATE_PATTERN = "\\d{8}";
    private static final String UUID_PATTERN = "[a-fA-F0-9]{16}";

    private VideoFileNameGenerator() {
    }

    public static String generateUniqueFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo original é inválido.");
        }

        int lastDot = originalFileName.lastIndexOf(".");
        String extension = lastDot != -1 ? originalFileName.substring(lastDot + 1) : "";
        String baseName = lastDot != -1 ? originalFileName.substring(0, lastDot) : originalFileName;

        baseName = sanitizeBaseName(baseName);
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        return baseName + "_" + date + "_" + uuid + (!extension.isBlank() ? "." + extension : "");
    }

    public static String generateFileNameWithSuffix(String originalFileName, String suffix) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo original é inválido.");
        }

        int lastDot = originalFileName.lastIndexOf(".");
        String extension = lastDot != -1 ? originalFileName.substring(lastDot + 1) : "";
        String baseName = lastDot != -1 ? originalFileName.substring(0, lastDot) : originalFileName;

        baseName = sanitizeBaseName(baseName);

        String[] parts = baseName.split("_");
        if (parts.length >= 3 &&
                parts[parts.length - 2].matches(DATE_PATTERN) &&
                parts[parts.length - 1].matches(UUID_PATTERN)) {
            baseName += "_" + suffix.toUpperCase();
        } else {
            String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            baseName = baseName + "_" + date + "_" + uuid + "_" + suffix.toUpperCase();
        }

        return baseName + (!extension.isBlank() ? "." + extension : "");
    }

    private static String sanitizeBaseName(String baseName) {
        return baseName.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
