package com.l8group.videoeditor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class VideoDurationUtils {

    /**
     * Obtém a duração de um vídeo no formato HH:mm:ss usando FFmpeg.
     *
     * @param filePath Caminho do arquivo de vídeo.
     * @return Duração do vídeo como string (HH:mm:ss).
     * @throws IOException Se houver erro ao executar o comando FFmpeg.
     */
    public static String getVideoDurationAsString(String filePath) throws IOException {
        String duration = executeFFmpegCommand(filePath);
        return formatDuration(duration);
    }

    /**
     * Executa o comando FFmpeg para obter a duração do vídeo.
     *
     * @param filePath Caminho do arquivo de vídeo.
     * @return Duração no formato HH:mm:ss.
     * @throws IOException Se houver erro ao executar o FFmpeg.
     */
    private static String executeFFmpegCommand(String filePath) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg", "-i", filePath);
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

    /**
     * Extrai a duração do vídeo a partir da saída do FFmpeg.
     *
     * @param line Linha de saída contendo a duração.
     * @return Duração no formato HH:mm:ss.
     */
    private static String extractDuration(String line) {
        String[] parts = line.split(",");
        for (String part : parts) {
            if (part.trim().startsWith("Duration:")) {
                String duration = part.replace("Duration:", "").trim(); // Remove "Duration:" e espaços
                return duration.split(" ")[0]; // Pega apenas "HH:mm:ss"
            }
        }
        return "00:00:00";
    }

    /**
     * Converte uma string de duração (HH:mm:ss) para um formato padronizado.
     *
     * @param duration Duração no formato HH:mm:ss.
     * @return String formatada no mesmo formato.
     */
    private static String formatDuration(String duration) {
        String[] timeParts = duration.trim().split(":"); // Remove espaços antes de dividir
        if (timeParts.length == 3) {
            int hours = Integer.parseInt(timeParts[0].trim()); // Remove espaços antes de converter
            int minutes = Integer.parseInt(timeParts[1].trim());
            int seconds = (int) Math.floor(Double.parseDouble(timeParts[2].trim()));
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return "00:00:00";
    }

    /**
     * Converte uma string no formato "HH:mm:ss" para segundos inteiros.
     *
     * @param time Tempo no formato "HH:mm:ss".
     * @return Tempo convertido em segundos.
     */
    public static int convertTimeToSeconds(String time) {
        String[] parts = time.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Formato inválido: " + time);
        }
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return (hours * 3600) + (minutes * 60) + seconds;
    }
    
    public static String formatSecondsToTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

}
