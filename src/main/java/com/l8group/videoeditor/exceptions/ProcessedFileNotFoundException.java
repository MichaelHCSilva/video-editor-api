package com.l8group.videoeditor.exceptions;

public class ProcessedFileNotFoundException extends RuntimeException {
    public ProcessedFileNotFoundException(String message) {
        super(message);
    }
}
