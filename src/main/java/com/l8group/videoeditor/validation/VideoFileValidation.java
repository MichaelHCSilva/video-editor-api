package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.utils.VideoValidationUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VideoFileValidation {

    public static void validateWithFFmpeg(String filePath) {
        log.info("Iniciando validações FFmpeg para o vídeo: {}", filePath);

        if (VideoValidationUtils.isVideoCorrupt(filePath)) {
            throw new IllegalArgumentException("O arquivo de vídeo parece estar corrompido e não pode ser processado. Por favor, verifique o arquivo ou tente enviar outro.");
        }
        if (!VideoValidationUtils.isValidVideoFormat(filePath)) {
            throw new IllegalArgumentException("O formato do arquivo de vídeo não é suportado. Os formatos aceitos são: MP4, AVI e MOV. Por favor, envie um arquivo em um desses formatos.");
        }
        if (VideoValidationUtils.hasBlackFrames(filePath)) {
            throw new IllegalArgumentException("O vídeo contém quadros totalmente pretos que podem indicar um problema. Se isso não for intencional, considere verificar o arquivo original.");
        }
        if (VideoValidationUtils.hasFrozenFrames(filePath)) {
            throw new IllegalArgumentException("O vídeo apresenta momentos de imagem congelada. Se isso não for intencional, pode haver um problema com o arquivo original.");
        }

        log.info("Validações FFmpeg concluídas com sucesso para: {}", filePath);
    }
}