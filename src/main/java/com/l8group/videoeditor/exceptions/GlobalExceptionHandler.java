package com.l8group.videoeditor.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VideoProcessingNotFoundException.class)
    public ResponseEntity<String> handleProcessingNotFound(VideoProcessingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(ProcessedFileNotFoundException.class)
    public ResponseEntity<String> handleFileNotFound(ProcessedFileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno no servidor.");
    }

    @ExceptionHandler(NoVideosFoundException.class)
    public ResponseEntity<String> handleNoVideosFound(NoVideosFoundException e) {
        return ResponseEntity.status(404).body("{\"message\": \"" + e.getMessage() + "\"}");
    }
}
