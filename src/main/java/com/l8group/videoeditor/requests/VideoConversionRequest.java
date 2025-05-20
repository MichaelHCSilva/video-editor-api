package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
public class VideoConversionRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório e não pode estar em branco.")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "O ID do vídeo deve ser um UUID válido no formato 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'.")
    private String videoId;

    @NotBlank(message = "O formato do video é obrigatório e não pode estar em branco.")
    @Pattern(regexp = "^(mp4|avi|mov)$", message = "Formatos suportados: mp4, avi, mov.")
    private String outputFormat;

    public VideoConversionRequest(String videoId, String outputFormat) {
        this.videoId = videoId;
        this.outputFormat = outputFormat;
    }
}