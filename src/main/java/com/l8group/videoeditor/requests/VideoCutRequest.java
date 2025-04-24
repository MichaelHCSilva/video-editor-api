package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoCutRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório.")
    private String videoId;

    @NotBlank(message = "O tempo de início é obrigatório.")
    @Pattern(regexp = "^\\d{2}:\\d{2}:\\d{2}$", message = "O horário de início deve estar no formato HH:mm:ss (apenas números).")
    private String startTime;

    @NotBlank(message = "O tempo de término é obrigatório.")
    @Pattern(regexp = "^\\d{2}:\\d{2}:\\d{2}$", message = "O horário de término deve estar no formato HH:mm:ss (apenas números).")
    private String endTime;
}