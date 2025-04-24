package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.exceptions.InvalidResizeParameterException;
import java.util.ArrayList;
import java.util.List;

public class VideoResizeValidator {

    public static void validateNumericWidthAndHeight(String widthStr, String heightStr) {
        List<String> errors = new ArrayList<>();

        // Validações para largura
        if (widthStr == null) {
            errors.add("A largura não pode ser nula.");
        } else if (widthStr.isEmpty()) {
            errors.add("A largura não pode estar vazia.");
        } else {
            try {
                int width = Integer.parseInt(widthStr);
                if (width < 0) {
                    errors.add("A largura não pode ser negativa.");
                }
            } catch (NumberFormatException e) {
                errors.add("A largura deve conter apenas números.");
            }
        }

        // Validações para altura
        if (heightStr == null) {
            errors.add("A altura não pode ser nula.");
        } else if (heightStr.isEmpty()) {
            errors.add("A altura não pode estar vazia.");
        } else {
            try {
                int height = Integer.parseInt(heightStr);
                if (height < 0) {
                    errors.add("A altura não pode ser negativa.");
                }
            } catch (NumberFormatException e) {
                errors.add("A altura deve conter apenas números.");
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidResizeParameterException(String.join(", ", errors));
        }
    }
}