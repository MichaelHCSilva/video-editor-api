package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.exceptions.InvalidResizeParameterException;
import com.l8group.videoeditor.utils.VideoResolutionsUtils;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;


@UtilityClass
public class VideoResizeValidation {

    /**
     * @param width 
     * @param height 
     */
    public static void validate(Integer width, Integer height) {
        List<String> errors = new ArrayList<>();

        if (width == null) {
            errors.add("A largura está ausente ou em formato inválido. Certifique-se de que seja um número inteiro.");
        }

        if (height == null) {
            errors.add("A altura está ausente ou em formato inválido. Certifique-se de que seja um número inteiro.");
        }

        if (width != null && height != null && !VideoResolutionsUtils.isValidResolution(width, height)) {
            errors.add("Resolução inválida. As resoluções suportadas são: " +
                    VideoResolutionsUtils.getSupportedResolutionsAsString() + ".");
        }

        if (!errors.isEmpty()) {
            throw new InvalidResizeParameterException(String.join(" ", errors));
        }
    }
}
