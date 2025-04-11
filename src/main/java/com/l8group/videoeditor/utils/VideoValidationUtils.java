package com.l8group.videoeditor.utils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class VideoValidationUtils {

    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("mp4", "avi", "mov");

    @Value("${video.validation.timeout.seconds}")
    private int timeoutValue;

    private static int validationTimeout;

    @PostConstruct
    private void init() {
        if (timeoutValue <= 0) {
            throw new IllegalArgumentException("O timeout de validação deve ser maior que zero.");
        }
        validationTimeout = timeoutValue;
    }

    public static boolean isVideoCorrupt(String filePath) {
        try {
            int exitCode = runProcess("ffmpeg", "-v", "error", "-i", filePath, "-f", "null", "-");
            boolean isCorrupt = exitCode != 0;
            log.info("Verificação de corrupção: {}. Corrompido: {}", filePath, isCorrupt);
            return isCorrupt;
        } catch (Exception e) {
            log.error("Erro ao verificar se o vídeo está corrompido: {}", filePath, e);
            return true; // Assumimos corrompido por segurança
        }
    }

    public static boolean hasBlackFrames(String filePath) {
        try {
            String output = runProcessAndGetError(
                    "ffmpeg", "-i", filePath, "-vf", "blackdetect=d=0.5:pix_th=0.10", "-f", "null", "-");
            boolean hasBlack = output.contains("black_start");
            log.info("Detecção de black frames: {}. Contém: {}", filePath, hasBlack);
            return hasBlack;
        } catch (Exception e) {
            log.error("Erro ao detectar black frames no vídeo: {}", filePath, e);
            return false;
        }
    }

    public static boolean hasFrozenFrames(String filePath) {
        try {
            String output = runProcessAndGetError(
                    "ffmpeg", "-i", filePath, "-vf", "freezedetect=n=-60dB", "-f", "null", "-");
            boolean hasFreeze = output.contains("freeze_start");
            log.info("Detecção de frozen frames: {}. Contém: {}", filePath, hasFreeze);
            return hasFreeze;
        } catch (Exception e) {
            log.error("Erro ao detectar frozen frames no vídeo: {}", filePath, e);
            return false;
        }
    }

    public static boolean isValidVideoFormat(String filePath) {
        try {
            String formatOutput = runProcessAndGetOutput(
                    "ffprobe", "-v", "error", "-select_streams", "v:0",
                    "-show_entries", "format=format_name",
                    "-of", "default=noprint_wrappers=1:nokey=1", filePath).trim().toLowerCase();

            log.info("Formato detectado com ffprobe: {}", formatOutput);

            // Suporte a múltiplos formatos detectados (ex: mov,mp4,m4a)
            String[] formats = formatOutput.split(",");
            for (String format : formats) {
                String cleanFormat = format.trim();
                if (SUPPORTED_FORMATS.contains(cleanFormat)) {
                    log.info("Formato suportado encontrado: {}", cleanFormat);
                    return true;
                }
            }

            log.warn("Nenhum dos formatos detectados é suportado: {}", formatOutput);
            return false;

        } catch (Exception e) {
            log.warn("FFprobe falhou para o vídeo '{}'. Tentando fallback por extensão...", filePath, e);
            return validateFormatByExtension(filePath);
        }
    }

    private static boolean validateFormatByExtension(String filePath) {
        String extension = FilenameUtils.getExtension(filePath).toLowerCase();
        boolean fallback = SUPPORTED_FORMATS.contains(extension);
        log.info("Formato por extensão: {}. Suportado: {}", extension, fallback);
        return fallback;
    }

    private static int runProcess(String... command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        boolean finished = process.waitFor(validationTimeout, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Timeout ao executar processo: " + Arrays.toString(command));
        }
        return process.exitValue();
    }

    private static String runProcessAndGetError(String... command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        boolean finished = process.waitFor(validationTimeout, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Timeout ao executar processo: " + Arrays.toString(command));
        }
        return new String(process.getErrorStream().readAllBytes());
    }

    private static String runProcessAndGetOutput(String... command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        boolean finished = process.waitFor(validationTimeout, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Timeout ao executar processo: " + Arrays.toString(command));
        }
        return new String(process.getInputStream().readAllBytes());
    }
}
