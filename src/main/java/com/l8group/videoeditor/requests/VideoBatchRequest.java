package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import com.l8group.videoeditor.enums.OverlayPositionEnum;

public class VideoBatchRequest {

    @NotEmpty(message = "A lista de vídeos não pode estar vazia.")
    private List<String> videoIds;

    @NotEmpty(message = "A lista de operações não pode estar vazia.")
    private List<BatchOperation> operations;

    public List<String> getVideoIds() {
        return videoIds;
    }

    public void setVideoIds(List<String> videoIds) {
        this.videoIds = videoIds;
    }

    public List<BatchOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<BatchOperation> operations) {
        this.operations = operations;
    }

    public static class BatchOperation {
        @NotNull(message = "O tipo de operação é obrigatório.")
        private String operationType;

        @NotNull(message = "Os parâmetros da operação são obrigatórios.")
        private OperationParameters parameters;

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public OperationParameters getParameters() {
            return parameters;
        }

        public void setParameters(OperationParameters parameters) {
            this.parameters = parameters;
        }
    }

    public static class OperationParameters {
        private String startTime; // Para CUT
        private String endTime; // Para CUT
        private Integer width; // Para RESIZE
        private Integer height; // Para RESIZE
        private String outputFormat; // Para CONVERT
        private String watermark; // Para OVERLAY
        private OverlayPositionEnum position; // Para OVERLAY
        private Integer fontSize; // Para OVERLAY

        // Getters e setters para todos os campos
        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        public String getOutputFormat() {
            return outputFormat;
        }

        public void setOutputFormat(String outputFormat) {
            this.outputFormat = outputFormat;
        }

        public String getWatermark() {
            return watermark;
        }

        public void setWatermark(String watermark) {
            this.watermark = watermark;
        }

        public OverlayPositionEnum getPosition() {
            return position;
        }

        public void setPosition(OverlayPositionEnum position) {
            this.position = position;
        }

        public Integer getFontSize() {
            return fontSize;
        }

        public void setFontSize(Integer fontSize) {
            this.fontSize = fontSize;
        }

    }

}
