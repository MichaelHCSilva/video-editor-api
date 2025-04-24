package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.requests.VideoOverlayRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class VideoOverlayValidator {

    private static final Logger log = LoggerFactory.getLogger(VideoOverlayValidator.class);
    private final Validator validator;

    public VideoOverlayValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    public void validate(VideoOverlayRequest request) {
        Set<ConstraintViolation<VideoOverlayRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(violation -> String.format("[%s: %s]", violation.getPropertyPath(), violation.getMessage()))
                    .collect(Collectors.joining(", "));
            log.warn("Erro na validação da requisição de overlay: {}", errorMessage);
            throw new ConstraintViolationException("Erro na validação da requisição de overlay: " + errorMessage, violations);
        }

        validateCustomFields(request);
    }

    private void validateCustomFields(VideoOverlayRequest request) {
        validateWatermark(request.getWatermark());

        // Só valida posição customizada se o método utilitário for mais complexo que o @Pattern
        // validatePosition(request.getPosition());

        validateFontSize(request.getFontSize());
    }

    private void validateWatermark(String watermark) {
        if (watermark != null && containsInvalidCharacters(watermark)) {
            String errorMessage = "O texto da marca d'água contém caracteres inválidos.";
            log.warn(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void validateFontSize(Integer fontSize) {
        if (fontSize != null && fontSize > 100) {
            String errorMessage = "O tamanho da fonte deve ser menor ou igual a 100.";
            log.warn(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private boolean containsInvalidCharacters(String text) {
        if (text == null) return false;
        // Permite letras, números e espaços de qualquer idioma
        Pattern pattern = Pattern.compile("[^\\p{L}\\p{N}\\s]");
        return pattern.matcher(text).find();
    }
}
