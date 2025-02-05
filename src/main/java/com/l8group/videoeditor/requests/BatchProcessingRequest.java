package com.l8group.videoeditor.requests;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class BatchProcessingRequest {

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
        private Object parameters;

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public Object getParameters() {
            return parameters;
        }

        public void setParameters(Object parameters) {
            this.parameters = parameters;
        }
    }
}
