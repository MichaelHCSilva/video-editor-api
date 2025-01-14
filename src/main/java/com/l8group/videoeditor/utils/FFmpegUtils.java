package com.l8group.videoeditor.utils;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class FFmpegUtils {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegUtils.class);
    private final FFprobe ffprobe;
    private final FFmpeg ffmpeg;

    public FFmpegUtils() {
        try {
            this.ffprobe = new FFprobe("/usr/bin/ffprobe");
            this.ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao inicializar FFmpeg ou FFprobe: " + e.getMessage(), e);
        }
    }

    public long getVideoDuration(MultipartFile file) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("video", ".tmp");
            file.transferTo(tempFile);

            FFmpegProbeResult probeResult = ffprobe.probe(tempFile.getAbsolutePath());
            Optional<FFmpegStream> videoStream = probeResult.getStreams().stream()
                    .filter(stream -> FFmpegStream.CodecType.VIDEO.equals(stream.codec_type))
                    .findFirst();

            if (videoStream.isEmpty()) {
                throw new IllegalArgumentException("Nenhum stream de vídeo encontrado no arquivo.");
            }

            long durationInSeconds = Math.round(videoStream.get().duration);
            logger.info("Duração do vídeo: {} segundos", durationInSeconds);
            return durationInSeconds;

        } catch (IOException e) {
            logger.error("Erro ao obter a duração do vídeo", e);
            throw new RuntimeException("Erro ao processar o vídeo: " + e.getMessage(), e);

        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public void cutVideo(File inputFile, File outputFile, long startTime, long endTime) {
        String inputPath = convertToLinuxPath(inputFile.getAbsolutePath());
        String outputPath = convertToLinuxPath(outputFile.getAbsolutePath());

        String[] command = {
                ffmpeg.getPath(),
                "-i", inputPath,
                "-ss", String.valueOf(startTime),
                "-to", String.valueOf(endTime),
                outputPath
        };

        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("FFmpeg Output: {}", line);
                }
            }

            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tempo limite excedido ao cortar o vídeo.");
            }

            if (process.exitValue() != 0) {
                throw new IllegalStateException("Erro ao cortar vídeo. Código de saída: " + process.exitValue());
            }
        } catch (IOException e) {
            logger.error("Erro ao executar o comando FFmpeg.", e);
            throw new RuntimeException("Erro ao executar o comando FFmpeg: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("O processo de corte de vídeo foi interrompido.", e);
            throw new RuntimeException("O processo de corte de vídeo foi interrompido: " + e.getMessage(), e);
        }
    }

    private String convertToLinuxPath(String windowsPath) {
        if (windowsPath.startsWith("C:\\")) {
            return windowsPath.replace("C:\\", "/mnt/c/").replace("\\", "/");
        }
        return windowsPath;
    }
}
