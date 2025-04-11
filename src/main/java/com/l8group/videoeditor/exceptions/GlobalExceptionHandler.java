package com.l8group.videoeditor.exceptions;

import java.nio.file.AccessDeniedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.l8group.videoeditor.responses.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VideoProcessingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProcessingNotFound(VideoProcessingNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Processamento de vídeo não encontrado.");
    }

    @ExceptionHandler(ProcessedFileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(ProcessedFileNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Arquivo processado não encontrado.");
    }

    @ExceptionHandler(NoVideosFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoVideosFound(NoVideosFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Nenhum vídeo encontrado.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Requisição inválida. Verifique os dados enviados.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acesso negado. Você não tem permissão para isso.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Parâmetro inválido fornecido.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno no servidor. Nossa equipe foi notificada.");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(message));
    }
}
