package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.exceptions.InvalidVideoIdListException;
import com.l8group.videoeditor.requests.VideoBatchRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VideoBatchValidation {

    public void validateRequest(VideoBatchRequest request) {
        if (request.getVideoIds() == null || request.getVideoIds().isEmpty()) {
            throw new InvalidVideoIdListException(
                "O campo obrigatório 'videoIds' está vazio ou não foi informado. " +
                "Por favor, informe ao menos um ID de vídeo para processar.");
        }

        if (request.getOperations() == null || request.getOperations().isEmpty()) {
            throw new IllegalArgumentException(
                "O campo obrigatório 'operations' está vazio ou não foi informado. " +
                "É necessário informar pelo menos uma operação a ser realizada.");
        }

        for (VideoBatchRequest.BatchOperation op : request.getOperations()) {
            validateOperation(op);
        }
    }

    private void validateOperation(VideoBatchRequest.BatchOperation operation) {
        String type = operation.getOperationType();
        VideoBatchRequest.OperationParameters params = operation.getParameters();

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                "O campo obrigatório 'operationType' está vazio ou não foi informado. " +
                "Por favor, informe o tipo da operação.");
        }

        if (params == null) {
            throw new IllegalArgumentException(
                "O campo obrigatório 'parameters' não foi informado. " +
                "É necessário fornecer os parâmetros para a operação.");
        }

        switch (type.toUpperCase()) {
            case "RESIZE":
            case "OVERLAY":
            case "CONVERT":
            case "CUT":
                break;
            default:
                throw new IllegalArgumentException(
                    "O tipo de operação '" + type + "' não é suportado. " +
                    "Tipos suportados: RESIZE, OVERLAY, CONVERT, CUT.");
        }
    }
}
