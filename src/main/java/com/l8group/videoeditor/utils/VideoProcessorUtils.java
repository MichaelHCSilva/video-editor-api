package com.l8group.videoeditor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoProcessorUtils {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessorUtils.class);

    public static boolean cutVideo(String inputFilePath, String outputFilePath, String startTime, String endTime) {
        logger.info("Iniciando corte do vídeo. inputFilePath={}, outputFilePath={}, startTime={}, endTime={}",
                inputFilePath, outputFilePath, startTime, endTime);

        boolean success = executeFFmpegCommand(
                "ffmpeg", "-i", inputFilePath,
                "-ss", startTime, "-to", endTime,
                "-c:v", "libx264", "-preset", "fast", "-crf", "23",
                "-c:a", "aac", "-b:a", "128k",
                "-movflags", "+faststart",
                "-map_metadata", "0",
                outputFilePath);

        if (success) {
            logger.info("Corte do vídeo concluído com sucesso: {}", outputFilePath);
        } else {
            logger.error("Falha no corte do vídeo: {}", outputFilePath);
        }
        return success;
    }

    public static boolean convertVideo(String inputFilePath, String outputFilePathWithoutExtension, String format) {
        logger.info("Iniciando conversão do vídeo. inputFilePath={}, outputFilePathWithoutExtension={}, formato={}",
                inputFilePath, outputFilePathWithoutExtension, format);

        String outputFilePathWithExtension = outputFilePathWithoutExtension + "." + format.toLowerCase();

        boolean success = executeFFmpegCommand(
                "ffmpeg", "-i", inputFilePath,
                "-c:v", "libx264", "-preset", "slow", "-crf", "23",
                "-c:a", "aac", "-b:a", "192k",
                "-movflags", "+faststart",
                "-map_metadata", "0",
                outputFilePathWithExtension);

        if (success) {
            logger.info("Conversão concluída com sucesso: {}", outputFilePathWithExtension);
        } else {
            logger.error("Falha na conversão do vídeo: {}", outputFilePathWithExtension);
        }
        return success;
    }

    public static boolean resizeVideo(String inputFilePath, String outputFilePath, int width, int height) {
        logger.info("Iniciando redimensionamento do vídeo. inputFilePath={}, outputFilePath={}, width={}, height={}",
                inputFilePath, outputFilePath, width, height);

        boolean success = executeFFmpegCommand(
                "ffmpeg", "-i", inputFilePath,
                "-vf", "scale=" + width + ":" + height,
                "-c:v", "libx264", "-preset", "fast", "-crf", "23",
                "-c:a", "aac", "-b:a", "128k",
                "-movflags", "+faststart",
                "-map_metadata", "0",
                outputFilePath);

        if (success) {
            logger.info("Redimensionamento concluído com sucesso: {}", outputFilePath);
        } else {
            logger.error("Falha no redimensionamento do vídeo: {}", outputFilePath);
        }
        return success;
    }

    private static boolean executeFFmpegCommand(String... command) {
        logger.info("Executando comando FFmpeg: {}", String.join(" ", command));

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            long startTime = System.currentTimeMillis();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String ffmpegOutput = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                int exitCode = process.waitFor();
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                if (exitCode != 0) {
                    logger.error("Erro ao executar FFmpeg. Código de saída: {}. Saída do processo:\n{}", exitCode,
                            ffmpegOutput);
                    return false;
                } else {
                    logger.info("Comando FFmpeg executado com sucesso em {} ms. Saída:\n{}", duration, ffmpegOutput);
                }
                return true;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Exceção ao executar FFmpeg: {}", e.getMessage(), e);
            return false;
        }
    }
}