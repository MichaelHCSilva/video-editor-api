package com.l8group.videoeditor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.l8group.videoeditor.models.VideoFile;

public class VideoDurationUtils {

    private static final String DEFAULT_DURATION = "00:00:00";

    // Método já existente para obter a duração do vídeo no formato HH:mm:ss
    public static String getVideoDurationAsString(String filePath) throws IOException {
        String duration = runFFmpegDurationCommand(filePath);
        return normalizeDurationFormat(duration);
    }

    // Novo método para obter a duração do vídeo em segundos, utilizando o VideoFile
    public static int getVideoDurationInSeconds(VideoFile videoFile) throws IOException {
        // Supondo que o VideoFile tem um método getFilePath() que retorna o caminho do arquivo
        String durationString = getVideoDurationAsString(videoFile.getVideoFilePath());
        return convertTimeToSeconds(durationString);
    }

    private static String runFFmpegDurationCommand(String filePath) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-i", filePath);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Duration:")) {
                    return extractDuration(line);
                }
            }
        }

        throw new IOException("Não foi possível obter a duração do vídeo.");
    }

    private static String extractDuration(String line) {
        String[] parts = line.split(",");
        for (String part : parts) {
            if (part.trim().startsWith("Duration:")) {
                return part.replace("Duration:", "").trim().split(" ")[0]; // Pega apenas "HH:mm:ss"
            }
        }
        return DEFAULT_DURATION;
    }

    private static String normalizeDurationFormat(String duration) {
        String[] timeParts = duration.trim().split(":");
        if (timeParts.length == 3) {
            int hours = Integer.parseInt(timeParts[0].trim());
            int minutes = Integer.parseInt(timeParts[1].trim());
            int seconds = (int) Math.floor(Double.parseDouble(timeParts[2].trim()));
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return DEFAULT_DURATION;
    }

    public static int convertTimeToSeconds(String time) {
        if (time == null || time.trim().isEmpty()) {
            throw new IllegalArgumentException("Formato de tempo inválido: Tempo não pode ser vazio.");
        }

        boolean isNegative = false;
        if (time.startsWith("-")) {
            isNegative = true;
            time = time.substring(1); // Remove o sinal negativo para o parsing
        }

        String[] parts = time.split(":");
        int hours = 0;
        int minutes = 0;
        int seconds = 0;

        try {
            switch (parts.length) {
                case 3:
                    hours = Integer.parseInt(parts[0]);
                    minutes = Integer.parseInt(parts[1]);
                    seconds = (int) Math.floor(Double.parseDouble(parts[2])); // Trunca a fração
                    break;
                case 2:
                    minutes = Integer.parseInt(parts[0]);
                    seconds = (int) Math.floor(Double.parseDouble(parts[1])); // Trunca a fração
                    break;
                case 1:
                    seconds = (int) Math.floor(Double.parseDouble(parts[0])); // Trunca a fração
                    break;
                default:
                    throw new IllegalArgumentException("Formato de tempo inválido: " + time);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato de tempo inválido: " + time + ". Certifique-se de usar números inteiros.");
        }

        int totalSeconds = (hours * 3600) + (minutes * 60) + seconds;
        return isNegative ? -totalSeconds : totalSeconds;
    }

    public static String formatSecondsToTime(int totalSeconds) {
        int absSeconds = Math.abs(totalSeconds);
        int hours = absSeconds / 3600;
        int minutes = (absSeconds % 3600) / 60;
        int seconds = absSeconds % 60;
        String sign = totalSeconds < 0 ? "-" : "";
        return String.format("%s%02d:%02d:%02d", sign, hours, minutes, seconds);
    }
}