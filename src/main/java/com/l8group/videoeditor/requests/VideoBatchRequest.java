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
    private List<@NotNull(message = "O ID do vídeo não pode ser nulo.") @NotEmpty(message = "O ID do vídeo não pode estar vazio.") String> videoIds;

    @NotEmpty(message = "A lista de operações a serem realizadas não pode estar vazia. Defina pelo menos uma operação (CUT, RESIZE, CONVERT, OVERLAY).")
    @Valid
    private List<BatchOperation> operations;

    @Data
    public static class BatchOperation {
        @NotNull(message = "O tipo da operação é obrigatório. Os tipos suportados são: CUT, RESIZE, CONVERT e OVERLAY.")
        private String operationType;

        @NotNull(message = "Os parâmetros específicos para a operação devem ser fornecidos.")
        @Valid
        private OperationParameters parameters;
    }

    @Data
    public static class OperationParameters {
        private String startTime;
        private String endTime;
        private String width;
        private String height;
        private String outputFormat;
        private String watermark;
        private String position;
        private Integer fontSize;

        public boolean isPositionValid() {
            return position == null || VideoOverlayPositionUtils.isValidPosition(position);
        }
    }
}