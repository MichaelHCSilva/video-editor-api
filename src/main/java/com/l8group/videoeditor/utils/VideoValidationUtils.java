package com.l8group.videoeditor.utils;

public class VideoValidationUtils {

    public static boolean isVideoCorrupt(String filePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg", "-v", "error", "-i", filePath, "-f", "null", "-"
            );
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode != 0; // Se exitCode for diferente de 0, o vídeo está corrompido
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean hasBlackFrames(String filePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg", "-i", filePath, "-vf", "blackdetect=d=0.5:pix_th=0.10", "-f", "null", "-"
            );
            Process process = processBuilder.start();
            String output = new String(process.getErrorStream().readAllBytes());
            return output.contains("black_start");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasFrozenFrames(String filePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg", "-i", filePath, "-vf", "freezedetect=n=-60dB", "-f", "null", "-"
            );
            Process process = processBuilder.start();
            String output = new String(process.getErrorStream().readAllBytes());
            return output.contains("freeze_start");
        } catch (Exception e) {
            return false;
        }
    }

    

    
}
