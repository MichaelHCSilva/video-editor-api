package com.l8group.videoeditor.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class VideoOverlayUtils {

    private static final Logger logger = LoggerFactory.getLogger(VideoOverlayUtils.class);

    public static boolean applyTextOverlayWithFFmpeg(String inputFilePath, String outputFilePath, String text,
                                                     String position, int fontSize, String fontFile) {
        try {
            if (!VideoOverlayPositionUtils.isValidPosition(position)) {
                logger.error("Posição inválida: {}", position);
                return false;
            }

            if (fontFile == null || fontFile.isEmpty()) {
                fontFile = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf";
            }

            String drawTextCommand = getDrawTextCommand(text, position, fontSize, fontFile);

            String ffmpegCommand = String.format(
                    "ffmpeg -i \"%s\" -vf \"%s\" -c:v libx264 -crf 20 -c:a aac -b:a 256k \"%s\"",
                    inputFilePath, drawTextCommand, outputFilePath
            );

            logger.info("Executando comando FFmpeg: {}", ffmpegCommand);

            if (!executeFFmpegCommand(ffmpegCommand)) {
                logger.error("Falha ao adicionar marca d'água. Comando: {}", ffmpegCommand);
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Erro ao adicionar marca d'água: {}", e.getMessage(), e);
            return false;
        }
    }

    private static String getDrawTextCommand(String text, String position, int fontSize, String fontFile) {
        String baseText = String.format(
                "drawtext=fontfile='%s':text='%s':fontsize=%d:box=1:boxcolor=black@0.5:boxborderw=5",
                fontFile, text, fontSize
        );

        String positionText = switch (position) {
            case VideoOverlayPositionUtils.TOP_LEFT -> ":x=10:y=10";
            case VideoOverlayPositionUtils.TOP_RIGHT -> ":x=w-tw-10:y=10";
            case VideoOverlayPositionUtils.BOTTOM_LEFT -> ":x=10:y=h-th-10";
            case VideoOverlayPositionUtils.BOTTOM_RIGHT -> ":x=w-tw-10:y=h-th-10";
            case VideoOverlayPositionUtils.CENTER -> ":x=(w-text_w)/2:y=(h-text_h)/2";
            default -> {
                logger.warn("Posição não reconhecida, usando posição padrão: center");
                yield ":x=(w-text_w)/2:y=(h-text_h)/2";
            }
        };

        String colorExpression = ":fontcolor=white";

        return baseText + positionText + colorExpression;
    }

    private static boolean executeFFmpegCommand(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String ffmpegOutput = reader.lines().collect(Collectors.joining(System.lineSeparator()));

                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    logger.error("Erro ao executar FFmpeg. Código {}: {}", exitCode, ffmpegOutput);
                    return false;
                } else {
                    logger.info("Saída do FFmpeg:\n{}", ffmpegOutput);
                }

                return true;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Exceção ao executar FFmpeg: {}", e.getMessage(), e);
            return false;
        }
    }
}
