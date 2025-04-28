package com.l8group.videoeditor.exceptions;

import lombok.Getter;

import java.util.List;

@Getter
public class BatchValidationException extends RuntimeException {

    private final List<String> errors;

    public BatchValidationException(List<String> errors) {
        super("Erro de validação no processamento batch.");
        this.errors = errors;
    }
}
