package com.l8group.videoeditor.exceptions;

import com.l8group.videoeditor.responses.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VideoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVideoNotFound(VideoNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(VideoProcessingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProcessingNotFound(VideoProcessingNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(ProcessedFileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(ProcessedFileNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(NoVideosFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoVideosFound(NoVideosFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .collect(Collectors.toList());
        return buildResponse(HttpStatus.BAD_REQUEST, errors, ex);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleWebExchangeBindException(WebExchangeBindException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        return buildResponse(HttpStatus.BAD_REQUEST, errors, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());
        return buildResponse(HttpStatus.BAD_REQUEST, errors, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        if (ex.getMessage().contains("Invalid UUID string: ")) {
            return buildResponse(HttpStatus.BAD_REQUEST, List.of("O ID de vídeo fornecido não é um UUID válido."), ex);
        }
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(InvalidCutTimeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCutTimeException(InvalidCutTimeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(VideoProcessingException.class)
    public ResponseEntity<ErrorResponse> handleVideoProcessingException(VideoProcessingException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(VideoMetadataException.class)
    public ResponseEntity<ErrorResponse> handleVideoMetadataException(VideoMetadataException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(InvalidMediaPropertiesException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMediaPropertiesException(InvalidMediaPropertiesException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(VideoDurationParseException.class)
    public ResponseEntity<ErrorResponse> handleVideoDurationParseException(VideoDurationParseException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(InvalidResizeParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidResizeParameterException(InvalidResizeParameterException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(InvalidVideoIdListException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVideoIdListException(InvalidVideoIdListException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFormat(HttpMessageNotReadableException ex) {
        String message = "Erro na leitura da requisição: verifique se todos os campos estão com os tipos corretos.";
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(message), ex);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, List<String> errors, Exception ex) {
        if (ex != null) {
            ex.printStackTrace();
        }
        return ResponseEntity.status(status).body(ErrorResponse.of(errors));
    }

    @ExceptionHandler(BatchValidationException.class)
    public ResponseEntity<Map<String, List<String>>> handleBatchValidationException(BatchValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", ex.getErrors())); // Pega a lista de erros da exceção
    }
}
