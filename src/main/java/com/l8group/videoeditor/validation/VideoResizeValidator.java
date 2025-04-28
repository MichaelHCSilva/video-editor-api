package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.exceptions.InvalidResizeParameterException;
import com.l8group.videoeditor.utils.VideoResolutionsUtils;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Validador de regras de negócio para redimensionamento de vídeos.
 * Aceita apenas resoluções pré-definidas em VideoResolutionsUtils.
 * Foca em validações semânticas e proteção contra entradas maliciosas.
 */
@UtilityClass
public class VideoResizeValidator {

    // Definido com base nas resoluções realmente suportadas
    private static final int MIN_RESOLUTION = 600;   // menor dimensão possível (600x600)
    private static final int MAX_RESOLUTION = 1920;  // maior dimensão possível (1920x1080 ou 1080x1920)

    /**
     * Valida a largura e altura solicitadas para redimensionamento de vídeo.
     *
     * @param width  Largura solicitada
     * @param height Altura solicitada
     */
    public static void validate(Integer width, Integer height) {
        List<String> errors = new ArrayList<>();

        // Validar se width/height são realmente não nulos
        if (width == null || height == null) {
            errors.add("Largura e altura não podem ser nulas.");
        } else {
            // Proteção contra valores absurdos ou manipulados
            if (width < MIN_RESOLUTION || width > MAX_RESOLUTION) {
                errors.add(String.format("Largura inválida: %d px. Deve estar entre %d e %d px.", width, MIN_RESOLUTION, MAX_RESOLUTION));
            }

            if (height < MIN_RESOLUTION || height > MAX_RESOLUTION) {
                errors.add(String.format("Altura inválida: %d px. Deve estar entre %d e %d px.", height, MIN_RESOLUTION, MAX_RESOLUTION));
            }

            // Validação semântica: combinação width x height deve ser permitida
            if (!VideoResolutionsUtils.isValidResolution(width, height)) {
                errors.add("Resolução inválida. Resoluções suportadas: " + VideoResolutionsUtils.getSupportedResolutionsAsString() + ".");
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidResizeParameterException(String.join(" ", errors));
        }
    }
}
