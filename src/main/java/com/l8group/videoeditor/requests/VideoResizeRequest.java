package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoResizeRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório")
    private String videoId;

    @Positive(message = "A largura informada não atende aos requisitos. Certifique-se de que é um valor positivo e compatível com as resoluções suportadas.")
    private int width;

    @Positive(message = "A altura informada não atende aos requisitos. Certifique-se de que é um valor positivo e compatível com as resoluções suportadas.")
    private int height;
}