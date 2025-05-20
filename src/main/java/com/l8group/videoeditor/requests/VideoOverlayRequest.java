package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoOverlayRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório e não pode estar em branco.")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "O ID do vídeo deve ser um UUID válido no formato 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'.")
    private String videoId;

    @NotBlank(message = "O texto da marca d'água é obrigatório e não pode estar em branco.")
    @Size(min = 1, max = 50, message = "O texto da marca d'água deve ter entre 1 e 50 caracteres.")
    private String watermark;

    @NotBlank(message = "A posição da marca d'água é obrigatória e não pode estar em branco.")
    @Pattern(regexp = "top-left|top-right|bottom-left|bottom-right|center", message = "A posição da marca d'água deve ser uma das seguintes: top-left, top-right, bottom-left, bottom-right, center.")
    private String position;

    @NotNull(message = "O tamanho da fonte é obrigatório e não pode estar em branco.")
    @Min(value = 1, message = "O tamanho da fonte deve ser maior que 0.")
    private Integer fontSize;
}
