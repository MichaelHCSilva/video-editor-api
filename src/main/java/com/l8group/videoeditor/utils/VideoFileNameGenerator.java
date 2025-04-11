package com.l8group.videoeditor.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class VideoFileNameGenerator {

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

        return baseName + "_" + date + "_" + uuid + "." + extension;
    }

    private static String sanitizeBaseName(String baseName) {
        return baseName.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
