package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Garante que o construtor sem argumentos ainda exista
public class VideoConversionRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório.")
    private String videoId;

    @NotBlank(message = "O formato de saída é obrigatório.")
    @Pattern(regexp = "^(mp4|avi|mov)$", message = "Formatos suportados: mp4, avi, mov.")
    private String outputFormat;

    public VideoConversionRequest(String videoId, String outputFormat) {
        this.videoId = videoId;
        this.outputFormat = outputFormat;
    }
}