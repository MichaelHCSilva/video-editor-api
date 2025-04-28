package com.l8group.videoeditor.requests;

import com.l8group.videoeditor.utils.VideoOverlayPositionUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class VideoBatchRequest {

    @NotEmpty(message = "Por favor, forneça a lista de IDs dos vídeos a serem processados.")
    @Size(min = 1, message = "A lista de IDs de vídeo deve conter pelo menos um ID.")
    private List<
        @NotNull(message = "O ID do vídeo não pode ser nulo.")
        @NotEmpty(message = "O ID do vídeo não pode estar vazio.")
        String> videoIds;

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
        private String startTime; // Para CUT
        private String endTime; // Para CUT
        private String width; // Para RESIZE
        private String height; // Para RESIZE
        private String outputFormat; // Para CONVERT
        private String watermark; // Para OVERLAY
        private String position; // Para OVERLAY
        private Integer fontSize; // Para OVERLAY

        public boolean isPositionValid() {
            return position == null || VideoOverlayPositionUtils.isValidPosition(position);
        }
    }
}