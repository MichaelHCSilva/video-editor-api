package com.l8group.videoeditor.requests;

import com.l8group.videoeditor.utils.VideoOverlayPositionUtils;
import jakarta.validation.Valid;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;

@Data
public class VideoBatchRequest {

    @NotEmpty(message = "Por favor, forneça a lista de IDs dos vídeos a serem processados.")
    private List<String> videoIds;

    @NotEmpty(message = "A lista de operações a serem realizadas não pode estar vazia. Defina pelo menos uma operação (CUT, RESIZE, CONVERT, OVERLAY).")
    @Valid // Valida os objetos BatchOperation dentro da lista
    private List<BatchOperation> operations;

    @Data
    public static class BatchOperation {
        @NotNull(message = "O tipo da operação é obrigatório. Os tipos suportados são: CUT, RESIZE, CONVERT e OVERLAY.")
        private String operationType;

        @NotNull(message = "Os parâmetros específicos para a operação devem ser fornecidos.")
        @Valid // Valida o objeto OperationParameters
        private OperationParameters parameters;
    }

    @Data
    public static class OperationParameters {
        @Pattern(regexp = "^\\d{2}:\\d{2}:\\d{2}$", message = "Formato de hora de início inválido (HH:MM:SS)")
        private String startTime; // Para CUT

        @Pattern(regexp = "^\\d{2}:\\d{2}:\\d{2}$", message = "Formato de hora de fim inválido (HH:MM:SS)")
        private String endTime; // Para CUT

        @Pattern(regexp = "^\\d+$", message = "A largura deve ser um número inteiro")
        private String width; // Para RESIZE

        @Pattern(regexp = "^\\d+$", message = "A altura deve ser um número inteiro")
        private String height; // Para RESIZE

        private String outputFormat; // Para CONVERT

        private String watermark; // Para OVERLAY

        private String position; // Para OVERLAY

        private Integer fontSize; // Para OVERLAY

        // Validação customizada para garantir que a posição da overlay é válida
        public boolean isPositionValid() {
            return position == null || VideoOverlayPositionUtils.isValidPosition(position);
        }
    }
}