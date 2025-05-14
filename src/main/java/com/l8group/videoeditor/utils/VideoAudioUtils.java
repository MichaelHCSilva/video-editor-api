package com.l8group.videoeditor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class VideoAudioUtils {

    private static final int PROCESS_TIMEOUT_SECONDS = 15;

    public static boolean hasAudioTrack(String filePath) throws IOException {
        String[] command = {
            "ffprobe", "-v", "error", "-select_streams", "a",
            "-show_entries", "stream=codec_type", "-of",
            "default=noprint_wrappers=1:nokey=1", filePath
        };

        Process process = new ProcessBuilder(command).start();
        waitForProcess(process);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines().anyMatch(line -> line.trim().equals("audio"));
        }
    }

    public static boolean isSilentSegment(String filePath, int start, int end) throws IOException {
        if (start >= end) {
            throw new IllegalArgumentException("O tempo inicial deve ser menor que o final.");
        }

        String[] command = {
            "ffmpeg", "-i", filePath, "-ss", String.valueOf(start), "-t", String.valueOf(end - start),
            "-af", "silencedetect=n=-30dB:d=1", "-f", "null", "-"
        };

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        waitForProcess(process);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines().noneMatch(line -> line.contains("silence_start"));
        }
    }

    public static boolean isAudioVideoSynced(String filePath) throws IOException {
        double videoStart = getStreamStartTime(filePath, "v:0");
        double audioStart = getStreamStartTime(filePath, "a:0");

        return Math.abs(videoStart - audioStart) < 0.5;
    }

    private static double getStreamStartTime(String filePath, String streamSpecifier) throws IOException {
        String[] command = {
            "ffprobe", "-v", "error", "-select_streams", streamSpecifier,
            "-show_entries", "stream=start_time", "-of", "default=noprint_wrappers=1:nokey=1", filePath
        };

        Process process = new ProcessBuilder(command).start();
        waitForProcess(process);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            return line != null ? Double.parseDouble(line.trim()) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0; 
        }
    }

    private static void waitForProcess(Process process) throws IOException {
        try {
            if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Timeout ao executar comando do sistema.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Execução interrompida.", e);
        }
    }

    
}
