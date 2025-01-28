package com.l8group.videoeditor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class VideoDurationUtils {

    public static Duration getVideoDuration(String videoFilePath) throws IOException, InterruptedException {
        String[] command = {
            "ffmpeg", 
            "-i", videoFilePath, 
            "2>&1"  
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);  
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            String durationLine = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("Duration")) {
                    durationLine = line;
                    break;  
                }
            }

            if (durationLine != null) {
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
            }
        }

        throw new IOException("Não foi possível obter a duração do vídeo.");
    }

    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds(); 
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, sec);
    }
}
