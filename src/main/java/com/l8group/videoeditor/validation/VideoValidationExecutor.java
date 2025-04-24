package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.utils.VideoValidationUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VideoValidationExecutor {

    public static void validateWithFFmpeg(String filePath) {
        log.info("Iniciando validações FFmpeg para o vídeo: {}", filePath);

        if (VideoValidationUtils.isVideoCorrupt(filePath)) {
            throw new IllegalArgumentException("O vídeo está corrompido e não pode ser processado.");
        }
        if (!VideoValidationUtils.isValidVideoFormat(filePath)) {
            throw new IllegalArgumentException("Formato de vídeo não suportado.");
        }
        if (VideoValidationUtils.hasBlackFrames(filePath)) {
            throw new IllegalArgumentException("O vídeo contém quadros pretos indesejados.");
        }
        if (VideoValidationUtils.hasFrozenFrames(filePath)) {
            throw new IllegalArgumentException("O vídeo apresenta congelamentos.");
        }

        log.info("Validações FFmpeg concluídas com sucesso para: {}", filePath);
    }
}
