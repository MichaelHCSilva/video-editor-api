package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import com.l8group.videoeditor.enums.OverlayPositionEnum;
import lombok.Data;

@Data
public class VideoBatchRequest {

    @NotEmpty(message = "A lista de vídeos não pode estar vazia.")
    private List<String> videoIds;

    @NotEmpty(message = "A lista de operações não pode estar vazia.")
    private List<BatchOperation> operations;

    @Data
    public static class BatchOperation {
        @NotNull(message = "O tipo de operação é obrigatório.")
        private String operationType;

        @NotNull(message = "Os parâmetros da operação são obrigatórios.")
        private OperationParameters parameters;
    }

    @Data
    public static class OperationParameters {
        private String startTime; // Para CUT
        private String endTime; // Para CUT
        private Integer width; // Para RESIZE
        private Integer height; // Para RESIZE
        private String outputFormat; // Para CONVERT
        private String watermark; // Para OVERLAY
        private OverlayPositionEnum position; // Para OVERLAY
        private Integer fontSize; // Para OVERLAY
    }
}