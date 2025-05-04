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
            return true;
        }
    }

    public static boolean hasBlackFrames(String filePath) {
        try {
            ProcessResult result = runProcessAndGetError(
                "ffmpeg", "-i", filePath, "-vf", "blackdetect=d=0.5:pix_th=0.10", "-f", "null", "-"
            );
            boolean detected = result.output.contains("black_start") && result.exitCode == 0;
            log.info("Detecção de black frames: {}. Contém: {}", filePath, detected);
            return detected;
        } catch (Exception e) {
            log.error("Erro ao detectar black frames no vídeo: {}", filePath, e);
            return false;
        }
    }

    public static boolean hasFrozenFrames(String filePath) {
        try {
            ProcessResult result = runProcessAndGetError(
                "ffmpeg", "-i", filePath, "-vf", "freezedetect=n=-60dB", "-f", "null", "-"
            );
            boolean detected = result.output.contains("freeze_start") && result.exitCode == 0;
            log.info("Detecção de frozen frames: {}. Contém: {}", filePath, detected);
            return detected;
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

            if (formatOutput.isEmpty()) {
                log.warn("ffprobe não retornou nenhuma informação de formato para o vídeo: {}", filePath);
            } else {
                log.info("Formato detectado com ffprobe: {}", formatOutput);
            }

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
        log.info("Executando o comando: {}", Arrays.toString(command));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        boolean finished = process.waitFor(validationTimeout, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            String errorMsg = "Timeout ao executar o processo: " + Arrays.toString(command);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("Comando executado com exit code {}: {}", exitCode, Arrays.toString(command));
        } else {
            log.info("Comando executado com sucesso: {}", Arrays.toString(command));
        }
        return exitCode;
    }

    private static ProcessResult runProcessAndGetError(String... command) throws IOException, InterruptedException {
        log.info("Executando o comando para capturar stderr: {}", Arrays.toString(command));
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        boolean finished = process.waitFor(validationTimeout, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            String errorMsg = "Timeout ao executar o processo: " + Arrays.toString(command);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        int exitCode = process.exitValue();
        String errorOutput = new String(process.getErrorStream().readAllBytes());
        return new ProcessResult(exitCode, errorOutput);
    }

    private static String runProcessAndGetOutput(String... command) throws IOException, InterruptedException {
        log.info("Executando o comando para capturar stdout: {}", Arrays.toString(command));
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        boolean finished = process.waitFor(validationTimeout, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            String errorMsg = "Timeout ao executar o processo: " + Arrays.toString(command);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        String output = new String(process.getInputStream().readAllBytes());
        log.info("Saída do comando {}: {}", Arrays.toString(command), output);
        return output;
    }

    public static boolean isRealVideo(String filePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v",
                "-show_entries", "stream=codec_type,duration,nb_frames",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
            );
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                return false;
            }

            return output.contains("video") && output.matches("(?s).*\\d+.*");
        } catch (Exception e) {
            log.error("Erro ao validar se é um vídeo real via FFmpeg: {}", e.getMessage(), e);
            return false;
        }
    }

    private record ProcessResult(int exitCode, String output) {}
}
