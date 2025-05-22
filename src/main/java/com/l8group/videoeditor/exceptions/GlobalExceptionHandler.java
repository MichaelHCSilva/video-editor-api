package com.l8group.videoeditor.exceptions;

import com.l8group.videoeditor.responses.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            VideoProcessingNotFoundException.class,
            ProcessedFileNotFoundException.class,
            NoVideosFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        String errorMessage = "Recurso não encontrado.";
        if (ex instanceof VideoProcessingNotFoundException) {
            errorMessage = ex.getMessage();
        } else if (ex instanceof ProcessedFileNotFoundException) {
            errorMessage = "O arquivo de vídeo processado não foi encontrado: " + ex.getMessage();
        } else if (ex instanceof NoVideosFoundException) {
            errorMessage = ex.getMessage();
        }
        return buildResponse(HttpStatus.NOT_FOUND, List.of(errorMessage), ex);
    }

    @ExceptionHandler({
            InvalidRequestException.class,
            InvalidCutTimeException.class,
            InvalidMediaPropertiesException.class,
            InvalidResizeParameterException.class,
            InvalidVideoIdListException.class,
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            WebExchangeBindException.class
    })
    public ResponseEntity<ErrorResponse> handleValidationException(Exception ex) {
        List<String> errors = (ex instanceof MethodArgumentNotValidException)
                ? ((MethodArgumentNotValidException) ex).getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .collect(Collectors.toList())
                : ((WebExchangeBindException) ex).getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        return buildResponse(HttpStatus.BAD_REQUEST, errors, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(v -> {
                    if (v.getMessage().startsWith("Operation error '")) {
                        return v.getMessage();
                    } else {
                        return "Erro de validação: " + v.getMessage();
                    }
                })
                .collect(Collectors.toList());
        return buildResponse(HttpStatus.BAD_REQUEST, errors, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, List.of("Acesso negado."), ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Invalid UUID string: ")) {
            return buildResponse(HttpStatus.BAD_REQUEST, List.of("O ID fornecido não é um UUID válido."), ex);
        }
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), ex);
    }

    @ExceptionHandler({
            VideoProcessingException.class,
            VideoMetadataException.class
    })
    public ResponseEntity<ErrorResponse> handleInternalProcessing(RuntimeException ex) {
        // ✅ Aqui usamos a mensagem original da exceção
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Erro interno ao processar o vídeo.";
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, List.of(errorMessage), ex);
    }

    @ExceptionHandler(BatchValidationException.class)
    public ResponseEntity<ErrorResponse> handleBatchValidation(BatchValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getErrors(), ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        String msg = "Erro na leitura da requisição: verifique se todos os campos estão com os tipos corretos.";
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(msg), ex);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                List.of("O campo 'file' é obrigatório e não foi enviado corretamente."), ex);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartError(MultipartException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                List.of("Requisição multipart malformada ou campo 'file' ausente/incorreto."), ex);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, List.of("Parâmetro obrigatório ausente: " + ex.getParameterName()),
                ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Exceção inesperada capturada no manipulador genérico: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, List.of("Erro inesperado no sistema."), ex);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, List<String> errors, Exception ex) {
        log.error("Exceção capturada (Status: {}): {}", status, ex.getMessage(), ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Content-Type-Options", "nosniff");
        headers.remove("Content-Disposition");
        headers.set("Content-Transfer-Encoding", "text");

        return ResponseEntity.status(status)
                .headers(headers)
                .body(ErrorResponse.of(errors));
    }
}
