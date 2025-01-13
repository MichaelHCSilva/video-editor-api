package com.l8group.videoeditor.utils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;

@Component
public class FFmpegUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FFmpegUtils.class);

    private final FFprobe ffprobe;

    public FFmpegUtils() {
        try {
            // Ajuste o caminho para o binário do FFprobe
            this.ffprobe = new FFprobe("/usr/bin/ffprobe");
        } catch (IOException e) {
            throw new RuntimeException("Erro ao inicializar FFprobe: " + e.getMessage(), e);
        }
    }

    public long getVideoDuration(MultipartFile file) {
        File tempFile = null;
        try {
            // Criando um arquivo temporário
            tempFile = File.createTempFile("video", ".tmp");
            file.transferTo(tempFile);

            // Usando o FFprobe para obter os dados do vídeo
            FFmpegProbeResult probeResult = ffprobe.probe(tempFile.getAbsolutePath());

            // Obtendo o primeiro stream de vídeo válido
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
            // Removendo o arquivo temporário
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
