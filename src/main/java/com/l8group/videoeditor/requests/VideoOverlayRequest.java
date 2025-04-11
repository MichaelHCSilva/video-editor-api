package com.l8group.videoeditor.requests;

import com.l8group.videoeditor.enums.OverlayPositionEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoOverlayRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório.")
    private String videoId;

    @NotBlank(message = "Os dados de sobreposição são obrigatórios.")
    @Size(max = 255, message = "Os dados de sobreposição devem ter no máximo 255 caracteres.")
    private String watermark;

    @NotNull(message = "A posição é obrigatória.")
    private OverlayPositionEnum position;

    @Min(value = 1, message = "O tamanho da fonte deve ser maior que 0.")
    private Integer fontSize;
}