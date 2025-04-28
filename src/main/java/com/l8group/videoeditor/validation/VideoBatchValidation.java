package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.exceptions.InvalidVideoIdListException;
import com.l8group.videoeditor.requests.VideoBatchRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VideoBatchValidation {

    public void validateRequest(VideoBatchRequest request) {
        if (request.getVideoIds() == null || request.getVideoIds().isEmpty()) {
            throw new InvalidVideoIdListException("Campo obrigatório 'videoIds' ausente ou vazio.");
        }

        if (request.getOperations() == null || request.getOperations().isEmpty()) {
            throw new IllegalArgumentException("Campo obrigatório 'operations' ausente ou vazio.");
        }

        for (VideoBatchRequest.BatchOperation op : request.getOperations()) {
            validateOperation(op);
        }
    }

    private void validateOperation(VideoBatchRequest.BatchOperation operation) {
        String type = operation.getOperationType();
        VideoBatchRequest.OperationParameters params = operation.getParameters();

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório 'operationType' ausente ou vazio.");
        }

        if (params == null) {
            throw new IllegalArgumentException("Campo obrigatório 'parameters' não foi fornecido.");
        }

        // A validação específica para cada tipo de operação foi removida daqui.
        // Espera-se que cada operação tenha sua própria validação.
        switch (type.toUpperCase()) {
            case "RESIZE":
            case "OVERLAY":
            case "CONVERT":
            case "CUT":
                // Nenhuma validação específica aqui. Assume-se que as classes de Request e Validator de cada operação farão isso.
                break;
            default:
                throw new IllegalArgumentException("Tipo de operação não suportado: '" + type + "'.");
        }
    }

    
}