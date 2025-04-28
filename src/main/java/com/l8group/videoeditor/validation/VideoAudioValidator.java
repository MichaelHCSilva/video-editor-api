package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.utils.VideoAudioUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class VideoAudioValidator {

    public static void validateAudioProperties(String filePath, int start, int end) {
        try {
            if (!VideoAudioUtils.hasAudioTrack(filePath)) {
                log.warn("O vídeo {} não possui trilha de áudio.", filePath);
            }

            if (VideoAudioUtils.isSilentSegment(filePath, start, end)) {
                log.warn("O segmento de {} até {} é silencioso no vídeo {}.", start, end, filePath);
            }

            if (!VideoAudioUtils.isAudioVideoSynced(filePath)) {
                log.warn("O vídeo {} está com áudio e vídeo dessincronizados.", filePath);
            }

        } catch (IOException e) {
            log.error("Erro durante a validação de áudio do vídeo {}: {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Erro ao validar propriedades de áudio do vídeo.", e);
        }
    }
}
