package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import com.l8group.videoeditor.enums.OverlayPositionEnum;
import lombok.Data;

@Data
public class VideoBatchRequest {

    @NotEmpty(message = "Por favor, forneça a lista de IDs dos vídeos a serem processados.")
    private List<String> videoIds;

    @NotEmpty(message = "A lista de operações a serem realizadas não pode estar vazia. Defina pelo menos uma operação (CUT, RESIZE, CONVERT, OVERLAY).")
    private List<BatchOperation> operations;

    @Data
    public static class BatchOperation {
        @NotNull(message = "O tipo da operação é obrigatório. Os tipos suportados são: CUT, RESIZE, CONVERT e OVERLAY.")
        private String operationType;

        @NotNull(message = "Os parâmetros específicos para a operação devem ser fornecidos.")
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
        private OverlayPositionEnum position; // Para OVERLAY
        private Integer fontSize; // Para OVERLAY
    }
}