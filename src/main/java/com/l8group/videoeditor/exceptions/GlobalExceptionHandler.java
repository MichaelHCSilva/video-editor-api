package com.l8group.videoeditor.exceptions;

import com.l8group.videoeditor.responses.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

    // 404 - Not Found
    @ExceptionHandler({
            VideoNotFoundException.class,
            VideoProcessingNotFoundException.class,
            ProcessedFileNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        String errorMessage = "O vídeo solicitado não foi encontrado.";
        if (ex instanceof VideoNotFoundException) {
            errorMessage = "O vídeo com o ID fornecido não foi encontrado.";
        }
        return buildResponse(HttpStatus.NOT_FOUND, List.of(errorMessage), ex);
    }

    // 400 - Bad Request para exceções específicas de validação de operação
    @ExceptionHandler({
            InvalidRequestException.class,
            InvalidCutTimeException.class,
            InvalidMediaPropertiesException.class,
            InvalidResizeParameterException.class,
            InvalidVideoIdListException.class,
            VideoDurationParseException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), ex);
    }

    // 400 - Validation errors (@Valid, WebFlux)
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

    // ✅ 400 - Constraint violations (ex: @Min, @Max etc.) - Agora com o padrão desejado
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(v -> {
                    String operationType = "";
                    // Tenta extrair o tipo de operação da mensagem, se disponível
                    if (v.getMessage().startsWith("Operation error '")) {
                        int start = "Operation error '".length();
                        int end = v.getMessage().indexOf("'", start);
                        if (end > start) {
                            operationType = v.getMessage().substring(start, end);
                        }
                        return v.getMessage();
                    } else {
                        return "Erro de validação: " + v.getMessage();
                    }
                })
                .collect(Collectors.toList());
        return buildResponse(HttpStatus.BAD_REQUEST, errors, ex);
    }

    // 403 - Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, List.of("Acesso negado."), ex);
    }

    // 400 - Illegal argument / UUID inválido
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Invalid UUID string: ")) {
            return buildResponse(HttpStatus.BAD_REQUEST, List.of("O ID de vídeo fornecido não é um UUID válido."), ex);
        }
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()), ex);
    }

    // 500 - Erro de processamento de vídeo
    @ExceptionHandler({
            VideoProcessingException.class,
            VideoMetadataException.class
    })
    public ResponseEntity<ErrorResponse> handleInternalProcessing(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Nenhum arquivo de vídeo encontrado")) {
            return buildResponse(HttpStatus.NOT_FOUND, List.of(ex.getMessage()), ex);
        }
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, List.of("Erro interno ao processar o vídeo."), ex);
    }

    // ✅ 400 - Validação em lote (padronizado)
    @ExceptionHandler(BatchValidationException.class)
    public ResponseEntity<ErrorResponse> handleBatchValidation(BatchValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getErrors(), ex);
    }

    // ✅ 400 - Erro de leitura do corpo JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        String msg = "Erro na leitura da requisição: verifique se todos os campos estão com os tipos corretos.";
        return buildResponse(HttpStatus.BAD_REQUEST, List.of(msg), ex);
    }

    // ✅ 400 - Parte 'file' ausente
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                List.of("O campo 'file' é obrigatório e não foi enviado corretamente."), ex);
    }

    // ✅ 400 - Multipart malformado
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartError(MultipartException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                List.of("Requisição multipart malformada ou campo 'file' ausente/incorreto."), ex);
    }

    // ✅ 400 - Parâmetro ausente
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, List.of("Parâmetro obrigatório ausente: " + ex.getParameterName()),
                ex);
    }

    // 500 - Fallback para qualquer exceção não mapeada
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, List.of("Erro inesperado no sistema."), ex);
    }

    // 🔁 Método centralizado com log e corpo padronizado
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, List<String> errors, Exception ex) {
        log.error("Exceção capturada: {}", ex.getMessage(), ex);
        return ResponseEntity.status(status).body(ErrorResponse.of(errors));
    }
}