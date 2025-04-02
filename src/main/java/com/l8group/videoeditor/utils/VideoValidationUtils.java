package com.l8group.videoeditor.utils;

public class VideoValidationUtils {

    public static boolean isVideoCorrupt(String filePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg", "-v", "error", "-i", filePath, "-f", "null", "-");
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
                    "ffmpeg", "-i", filePath, "-vf", "blackdetect=d=0.5:pix_th=0.10", "-f", "null", "-");
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
                    "ffmpeg", "-i", filePath, "-vf", "freezedetect=n=-60dB", "-f", "null", "-");
            Process process = processBuilder.start();
            String output = new String(process.getErrorStream().readAllBytes());
            return output.contains("freeze_start");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidVideoFormat(String filePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "format=format_name",
                    "-of", "default=noprint_wrappers=1:nokey=1", filePath);
            Process process = processBuilder.start();
            String format = new String(process.getInputStream().readAllBytes()).trim().toLowerCase();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                return false; // FFmpeg não conseguiu identificar o formato
            }

            return format.equals("mp4") || format.equals("avi") || format.equals("mov");
        } catch (Exception e) {
            return false; // Em caso de erro, assume que o formato é inválido
        }
    }

}
