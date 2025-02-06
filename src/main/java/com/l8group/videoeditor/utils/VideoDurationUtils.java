package com.l8group.videoeditor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class VideoDurationUtils {

    private static final Logger logger = LoggerFactory.getLogger(VideoDurationUtils.class);
    private static final int TIMEOUT_SECONDS = 10;

    @Async("taskExecutor")
    public CompletableFuture<Duration> getVideoDurationAsync(String videoFilePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getVideoDuration(videoFilePath);
            } catch (IOException | InterruptedException e) {
                logger.error("Erro ao obter a duração do vídeo: {}", e.getMessage());
                return Duration.ZERO;
            }
        });
    }

    public static Duration getVideoDuration(String videoFilePath) throws IOException, InterruptedException {
        String[] command = {"ffmpeg", "-i", videoFilePath, "2>&1"};

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Duration")) {
                    return parseDuration(line);
                }
            }
        }

        boolean completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!completed) {
            process.destroy();
            throw new IOException("Tempo limite excedido ao obter duração do vídeo.");
        }

        throw new IOException("Não foi possível obter a duração do vídeo.");
    }

    private static Duration parseDuration(String durationLine) {
        try {
            String durationPart = durationLine.split("Duration: ")[1].split(",")[0].trim();
            String[] durationParts = durationPart.split(":");
    
            if (durationParts.length == 3) {
                int hours = Integer.parseInt(durationParts[0]);
                int minutes = Integer.parseInt(durationParts[1]);
                double seconds = Double.parseDouble(durationParts[2]);
    
                long totalSeconds = TimeUnit.HOURS.toSeconds(hours)
                        + TimeUnit.MINUTES.toSeconds(minutes)
                        + (long) seconds;
                return Duration.ofSeconds(totalSeconds);
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException | NullPointerException e) {
            logger.error("Erro ao analisar a duração do vídeo: {}", e.getMessage());
        }
        return Duration.ZERO;
    }
    
    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, sec);
    }
}
