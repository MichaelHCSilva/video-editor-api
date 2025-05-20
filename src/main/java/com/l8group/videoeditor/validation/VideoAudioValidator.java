package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.utils.VideoAudioUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class VideoAudioValidator {

    public static void validateAudioProperties(String filePath, int start, int end) {
        try {
            if (!VideoAudioUtils.hasAudioTrack(filePath)) {
                log.warn("Aviso: O vídeo em '{}' não contém nenhuma trilha de áudio detectada.", filePath);
                throw new IllegalArgumentException("O vídeo não possui trilha de áudio.");
            }

            if (VideoAudioUtils.isSilentSegment(filePath, start, end)) {
                log.warn("Aviso: O segmento de áudio do vídeo '{}' entre os segundos {} e {} foi detectado como silencioso.", filePath, start, end);
                throw new IllegalArgumentException(String.format("O segmento de %d a %d é silencioso.", start, end));
            }

            if (!VideoAudioUtils.isAudioVideoSynced(filePath)) {
                log.warn("Aviso: O áudio e o vídeo do arquivo '{}' foram detectados como dessincronizados.", filePath);
                throw new IllegalArgumentException("O áudio e o vídeo estão dessincronizados.");
            }

        } catch (IOException e) {
            log.error("Erro ao validar propriedades de áudio do vídeo {}: {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Ocorreu um erro ao verificar as propriedades de áudio do vídeo. Por favor, tente novamente.", e);
        }
    }
}