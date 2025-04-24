package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoResizeRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório")
    private String videoId;

    @NotNull(message = "A largura é obrigatória.")
    @Positive(message = "A largura informada não atende aos requisitos.")
    private Integer width;

    @NotNull(message = "A altura é obrigatória.")
    @Positive(message = "A altura informada não atende aos requisitos.")
    private Integer height;
}