package com.l8group.videoeditor.services;

import com.l8group.videoeditor.exceptions.BatchValidationException;
import com.l8group.videoeditor.exceptions.InvalidCutTimeException;
import com.l8group.videoeditor.exceptions.InvalidResizeParameterException;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.requests.*;
import com.l8group.videoeditor.validation.VideoResizeValidation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoOperationService {

    private final VideoCutService videoCutService;
    private final VideoResizeService videoResizeService;
    private final VideoOverlayService videoOverlayService;
    private final VideoConversionService videoConversionService;
    private final Validator validator;
    private final VideoFileFinderService videoFileFinderService;

    private static final List<String> SUPPORTED_OPERATIONS = List.of("CUT", "RESIZE", "OVERLAY", "CONVERT");
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}$");

    public String execute(String videoId,
                          List<VideoBatchRequest.BatchOperation> operations,
                          String currentInputFilePath,
                          String currentOutputFormat) {

        log.info("[execute] Iniciando processamento do vídeo ID: {} | Input: {}", videoId, currentInputFilePath);

        validateAllOperations(videoId, operations);

        String outputFilePath = currentInputFilePath;

        for (VideoBatchRequest.BatchOperation operation : operations) {
            log.info("[execute] Executando operação: {} | Vídeo ID: {} | Caminho atual: {}",
                    operation.getOperationType(), videoId, outputFilePath);

            try {
                outputFilePath = switch (operation.getOperationType().toUpperCase()) {
                    case "CUT" -> handleCutOperation(videoId, operation.getParameters(), outputFilePath);
                    case "RESIZE" -> handleResizeOperation(videoId, operation.getParameters(), outputFilePath);
                    case "OVERLAY" -> handleOverlayOperation(videoId, operation.getParameters(), outputFilePath);
                    case "CONVERT" -> handleConvertOperation(videoId, operation.getParameters(), outputFilePath);
                    default -> throw new IllegalArgumentException("Operação inválida: " + operation.getOperationType()
                            + ". Os tipos suportados são: " + String.join(", ", SUPPORTED_OPERATIONS) + ".");
                };
            } catch (Exception e) {
                log.error("[execute] Erro ao executar operação {}: {}", operation.getOperationType(), e.getMessage(),
                        e);
                throw e;
            }
        }

        log.info("[execute] Processamento finalizado para vídeo ID: {} | Arquivo final: {}", videoId, outputFilePath);
        return outputFilePath;
    }

    public void validateAllOperations(String videoId, List<VideoBatchRequest.BatchOperation> operations) {
        log.info("[validateAllOperations] Iniciando validação prévia para vídeo ID: {}", videoId);
        List<String> errorMessages = new ArrayList<>();
        VideoFile videoFile = videoFileFinderService.findById(videoId);

        for (VideoBatchRequest.BatchOperation operation : operations) {
            String opType = operation.getOperationType().toUpperCase();
            log.info("[validateAllOperations] Validando operação: {}", opType);

            try {
                switch (opType) {
                    case "CUT" -> {
                        log.debug("[validateAllOperations] CUT params: start={}, end={}",
                                operation.getParameters().getStartTime(), operation.getParameters().getEndTime());
                        VideoCutRequest cutRequest = new VideoCutRequest(
                                videoId,
                                operation.getParameters().getStartTime(),
                                operation.getParameters().getEndTime());
                        validateRequest(cutRequest);
                        videoCutService.validateCutTimes(cutRequest, videoFile);
                    }
                    case "RESIZE" -> {
                        Integer width = parseInteger(operation.getParameters().getWidth());
                        Integer height = parseInteger(operation.getParameters().getHeight());
                        log.debug("[validateAllOperations] RESIZE params: width={}, height={}", width, height);
                        validateRequest(new VideoResizeRequest(videoId, width, height));
                        VideoResizeValidation.validate(width, height);
                    }
                    case "OVERLAY" -> {
                        log.debug("[validateAllOperations] OVERLAY params: text={}, position={}, fontSize={}",
                                operation.getParameters().getWatermark(),
                                operation.getParameters().getPosition(),
                                operation.getParameters().getFontSize());
                        validateRequest(new VideoOverlayRequest(
                                videoId,
                                operation.getParameters().getWatermark(),
                                operation.getParameters().getPosition(),
                                operation.getParameters().getFontSize()));
                    }
                    case "CONVERT" -> {
                        log.debug("[validateAllOperations] CONVERT params: format={}",
                                operation.getParameters().getOutputFormat());
                        validateRequest(new VideoConversionRequest(
                                videoId,
                                operation.getParameters().getOutputFormat()));
                    }
                    default -> {
                        log.warn("[validateAllOperations] Operação desconhecida: {}", opType);
                        throw new IllegalArgumentException("Código de operação inválido. Os códigos aceitos são: "
                                + String.join(", ", SUPPORTED_OPERATIONS) + ".");
                    }
                }
            } catch (ConstraintViolationException e) {
                for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
                    String error = "Operation error '" + operation.getOperationType().toUpperCase() + "': "
                            + violation.getMessage();
                    errorMessages.add(error);
                    log.warn("[validateAllOperations] Violação de constraint: {}", error);
                }
            } catch (IllegalArgumentException e) {
                String error = "Operation error '" + operation.getOperationType().toUpperCase() + "': "
                        + e.getMessage();
                errorMessages.add(error);
                log.warn("[validateAllOperations] Argumento inválido: {}", error);
            } catch (InvalidResizeParameterException e) {
                String error = "Operation error 'RESIZE': " + e.getMessage();
                errorMessages.add(error);
                log.warn("[validateAllOperations] Erro de resolução inválida: {}", error);
            } catch (InvalidCutTimeException e) {
                String error = "Operation error 'CUT': " + e.getMessage();
                errorMessages.add(error);
                log.warn("[validateAllOperations] Erro de tempo de corte inválido: {}", error);
            } catch (Exception e) {
                String error = "Operation error '" + operation.getOperationType().toUpperCase() + "': "
                        + e.getMessage();
                errorMessages.add(error);
                log.error("[validateAllOperations] Erro inesperado na operação {}: {}", opType, e.getMessage(), e);
            }
        }

        if (!errorMessages.isEmpty()) {
            log.error("[validateAllOperations] Erros de validação encontrados: {}", errorMessages);
            throw new BatchValidationException(errorMessages);
        }

        log.info("[validateAllOperations] Todas as operações validadas com sucesso.");
    }

    private void validateRequest(Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            log.warn("[validateRequest] Violações encontradas: {}", violations);
            throw new ConstraintViolationException(violations);
        }
        if (request instanceof VideoCutRequest cutRequest) {
            if (!isValidTimeString(cutRequest.getStartTime()) || !isValidTimeString(cutRequest.getEndTime())) {
                throw new InvalidCutTimeException("Os tempos de corte devem estar no formato HH:MM:SS.");
            }
        }
    }

    private String handleCutOperation(String videoId, VideoBatchRequest.OperationParameters parameters,
                                      String inputPath) {
        log.info("[handleCutOperation] Processando corte para vídeo ID: {} | start: {} | end: {}",
                videoId, parameters.getStartTime(), parameters.getEndTime());

        VideoCutRequest request = new VideoCutRequest(videoId, parameters.getStartTime(), parameters.getEndTime());
        return videoCutService.cutVideo(request, inputPath);
    }

    private String handleResizeOperation(String videoId, VideoBatchRequest.OperationParameters parameters,
                                         String inputPath) {
        log.info("[handleResizeOperation] Processando redimensionamento para vídeo ID: {} | width: {} | height: {}",
                videoId, parameters.getWidth(), parameters.getHeight());

        Integer width = parseInteger(parameters.getWidth());
        Integer height = parseInteger(parameters.getHeight());

        VideoResizeValidation.validate(width, height);
        VideoResizeRequest request = new VideoResizeRequest(videoId, width, height);
        return videoResizeService.resizeVideo(request, inputPath);
    }

    private String handleOverlayOperation(String videoId, VideoBatchRequest.OperationParameters parameters,
                                          String inputPath) {
        log.info(
                "[handleOverlayOperation] Aplicando overlay para vídeo ID: {} | texto: {} | posição: {} | fontSize: {}",
                videoId, parameters.getWatermark(), parameters.getPosition(), parameters.getFontSize());

        VideoOverlayRequest request = new VideoOverlayRequest(videoId, parameters.getWatermark(),
                parameters.getPosition(), parameters.getFontSize());
        return videoOverlayService.processOverlay(request, inputPath);
    }

    private String handleConvertOperation(String videoId, VideoBatchRequest.OperationParameters parameters,
                                           String inputPath) {
        log.info("[handleConvertOperation] Convertendo vídeo ID: {} | formato de saída: {}",
                videoId, parameters.getOutputFormat());

        VideoConversionRequest request = new VideoConversionRequest(videoId, parameters.getOutputFormat());
        return videoConversionService.convertVideo(request, inputPath);
    }

    private Integer parseInteger(String value) {
        try {
            Integer parsed = value == null ? null : Integer.parseInt(value);
            log.debug("[parseInteger] Sucesso ao converter '{}': {}", value, parsed);
            return parsed;
        } catch (NumberFormatException e) {
            log.warn("[parseInteger] Falha ao converter valor para Integer: {}", value, e);
            return null;
        }
    }

    private boolean isValidTimeString(String time) {
        return time != null && TIME_PATTERN.matcher(time).matches();
    }
}