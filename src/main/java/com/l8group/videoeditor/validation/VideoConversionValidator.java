package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.exceptions.InvalidRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class VideoConversionValidator {

    // Valida o ID do vídeo (não permite caracteres especiais ou espaços em branco)
    public void validateVideoId(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            throw new InvalidRequestException("O ID do vídeo não pode ser vazio.");
        }
        if (!Pattern.matches("^[a-fA-F0-9-]{36}$", videoId)) { // Valida se é um UUID válido
            throw new InvalidRequestException("O ID do vídeo deve ser um UUID válido.");
        }
    }

    // Valida o formato de saída (deve ser mp4, avi ou mov)
    public void validateOutputFormat(String outputFormat) {
        if (outputFormat == null || outputFormat.trim().isEmpty()) {
            throw new InvalidRequestException("O formato de saída não pode ser vazio.");
        }
        if (!Pattern.matches("^(mp4|avi|mov)$", outputFormat.toLowerCase())) {
            throw new InvalidRequestException("Formatos suportados: mp4, avi, mov.");
        }
    }

    // Valida se o nome do arquivo não contém caracteres especiais
    public void validateFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new InvalidRequestException("O nome do arquivo não pode ser vazio.");
        }
        if (!Pattern.matches("^[a-zA-Z0-9._-]+$", fileName)) { // Permite apenas letras, números, _ , . e -
            throw new InvalidRequestException("O nome do arquivo contém caracteres inválidos.");
        }
    }
}
