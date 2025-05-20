package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoResizeRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório e não pode estar em branco.")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "O ID do vídeo deve ser um UUID válido no formato 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'.")
    private String videoId;

    @NotNull(message = "A largura desejada para redimensionamento é obrigatória e não pode estar em branco.")
    @Positive(message = "A largura deve ser um número positivo.")
    private Integer width;

    @NotNull(message = "A altura desejada para redimensionamento é obrigatória e não pode estar em branco.")
    @Positive(message = "A altura deve ser um número positivo.")
    private Integer height;
}