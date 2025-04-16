package com.l8group.videoeditor.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class VideoFileNameGenerator {

    private static final String DATE_PATTERN = "\\d{8}";
    private static final String UUID_PATTERN = "[a-fA-F0-9]{16}";

    private VideoFileNameGenerator() {
    }

    // Gera o nome inicial com data e UUID
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

    // Gera nome com sufixo, reutilizando base com data+UUID
    public static String generateFileNameWithSuffix(String originalFileName, String suffix) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo original é inválido.");
        }

        int lastDot = originalFileName.lastIndexOf(".");
        String extension = lastDot != -1 ? originalFileName.substring(lastDot + 1) : "";
        String baseName = lastDot != -1 ? originalFileName.substring(0, lastDot) : originalFileName;

        baseName = sanitizeBaseName(baseName);

        // Verifica se o nome já possui data e UUID
        String[] parts = baseName.split("_");
        if (parts.length >= 3 &&
                parts[parts.length - 2].matches(DATE_PATTERN) &&
                parts[parts.length - 1].matches(UUID_PATTERN)) {
            // OK, já tem data e UUID – apenas adiciona o sufixo
            baseName += "_" + suffix.toUpperCase();
        } else {
            // Gera um novo nome com sufixo
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
