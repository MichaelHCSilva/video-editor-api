package com.l8group.videoeditor.services;

import com.l8group.videoeditor.exceptions.BatchValidationException;
import com.l8group.videoeditor.requests.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoOperationExecutor {

    private final VideoCutService videoCutService;
    private final VideoResizeService videoResizeService;
    private final VideoOverlayService videoOverlayService;
    private final VideoConversionService videoConversionService;
    private final Validator validator;

    public String execute(String videoId,
                          List<VideoBatchRequest.BatchOperation> operations,
                          String currentInputFilePath,
                          String currentOutputFormat) {

        log.info("[execute] Iniciando processamento do vídeo ID: {} | Input: {}", videoId, currentInputFilePath);

        // Validação antecipada de todas as operações
        validateAllOperations(videoId, operations);

        // Processa as operações após a validação
        String outputFilePath = currentInputFilePath; // Inicializa com o caminho do arquivo de entrada
        for (VideoBatchRequest.BatchOperation operation : operations) {
            log.info("[execute] Executando operação: {} | Video ID: {} | Input: {}",
                    operation.getOperationType(), videoId, outputFilePath);

            outputFilePath = switch (operation.getOperationType().toUpperCase()) {
                case "CUT" -> handleCutOperation(videoId, operation.getParameters(), outputFilePath);
                case "RESIZE" -> handleResizeOperation(videoId, operation.getParameters(), outputFilePath);
                case "OVERLAY" -> handleOverlayOperation(videoId, operation.getParameters(), outputFilePath);
                case "CONVERT" -> handleConvertOperation(videoId, operation.getParameters(), outputFilePath);
                default -> throw new IllegalArgumentException("Operação inválida: " + operation.getOperationType()
                        + ". Os tipos suportados são: CUT, RESIZE, CONVERT e OVERLAY.");
            };
        }

        return outputFilePath;
    }

    // Valida todas as operações no batch antes de executar qualquer uma delas
    public void validateAllOperations(String videoId, List<VideoBatchRequest.BatchOperation> operations) {
        List<String> errorMessages = new ArrayList<>();

        for (VideoBatchRequest.BatchOperation operation : operations) {
            try {
                // Validar os parâmetros específicos de cada operação
                switch (operation.getOperationType().toUpperCase()) {
                    case "CUT" -> validateRequest(new VideoCutRequest(
                            videoId, operation.getParameters().getStartTime(), operation.getParameters().getEndTime()));
                    case "RESIZE" -> validateRequest(new VideoResizeRequest(
                            videoId, parseInteger(operation.getParameters().getWidth()), parseInteger(operation.getParameters().getHeight())));
                    case "OVERLAY" -> validateRequest(new VideoOverlayRequest(
                            videoId, operation.getParameters().getWatermark(), operation.getParameters().getPosition(), operation.getParameters().getFontSize()));
                    case "CONVERT" -> validateRequest(new VideoConversionRequest(
                            videoId, operation.getParameters().getOutputFormat()));
                    default -> throw new IllegalArgumentException("Operação inválida: " + operation.getOperationType());
                }
            } catch (ConstraintViolationException e) {
                for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
                    errorMessages.add("Operation error '" + operation.getOperationType() + "': " + violation.getMessage());
                }
            } catch (IllegalArgumentException e) {
                errorMessages.add("Operation error '" + operation.getOperationType() + "': " + e.getMessage());
            } catch (Exception e) {
                errorMessages.add("Operation error '" + operation.getOperationType() + "': " + e.getMessage());
            }
        }

        if (!errorMessages.isEmpty()) {
            throw new BatchValidationException(errorMessages);
        }
    }

    // Método de validação individual das requisições
    private void validateRequest(Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    // Métodos de tratamento para cada operação
    private String handleCutOperation(String videoId, VideoBatchRequest.OperationParameters parameters, String inputPath) {
        VideoCutRequest request = new VideoCutRequest(videoId, parameters.getStartTime(), parameters.getEndTime());
        validateRequest(request);
        return videoCutService.cutVideo(request, inputPath);
    }

    private String handleResizeOperation(String videoId, VideoBatchRequest.OperationParameters parameters, String inputPath) {
        Integer width = parseInteger(parameters.getWidth());
        Integer height = parseInteger(parameters.getHeight());
        VideoResizeRequest request = new VideoResizeRequest(videoId, width, height);
        validateRequest(request);
        return videoResizeService.resizeVideo(request, inputPath);
    }

    private String handleOverlayOperation(String videoId, VideoBatchRequest.OperationParameters parameters, String inputPath) {
        VideoOverlayRequest request = new VideoOverlayRequest(videoId, parameters.getWatermark(), parameters.getPosition(), parameters.getFontSize());
        validateRequest(request);
        return videoOverlayService.processOverlay(request, inputPath);
    }

    private String handleConvertOperation(String videoId, VideoBatchRequest.OperationParameters parameters, String inputPath) {
        VideoConversionRequest request = new VideoConversionRequest(videoId, parameters.getOutputFormat());
        validateRequest(request);
        return videoConversionService.convertVideo(request, inputPath);
    }

    // Método para conversão segura de String para Integer
    private Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Falha ao converter valor para Integer: {}", value, e);
            return null;
        }
    }
}
