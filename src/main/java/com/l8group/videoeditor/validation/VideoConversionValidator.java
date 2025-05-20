package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.exceptions.InvalidRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class VideoConversionValidator {

    public void validateVideoId(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            throw new InvalidRequestException("O ID do vídeo é obrigatório e não pode estar vazio.");
        }
        if (!Pattern.matches("^[a-fA-F0-9-]{36}$", videoId)) { 
            throw new InvalidRequestException("O ID do vídeo informado é inválido. Deve ser um UUID válido (ex: 123e4567-e89b-12d3-a456-426614174000).");
        }
    }

    public void validateOutputFormat(String outputFormat) {
        if (outputFormat == null || outputFormat.trim().isEmpty()) {
            throw new InvalidRequestException("O formato de saída é obrigatório e não pode estar vazio.");
        }
        if (!Pattern.matches("^(mp4|avi|mov)$", outputFormat.toLowerCase())) {
            throw new InvalidRequestException("Formato de saída inválido. Aceitamos apenas: mp4, avi, mov.");
        }
    }

    public void validateFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new InvalidRequestException("O nome do arquivo é obrigatório e não pode estar vazio.");
        }
        if (!Pattern.matches("^[a-zA-Z0-9._-]+$", fileName)) { 
            throw new InvalidRequestException("O nome do arquivo contém caracteres inválidos. Use apenas letras, números, '.', '-' ou '_'.");
        }
    }
}
