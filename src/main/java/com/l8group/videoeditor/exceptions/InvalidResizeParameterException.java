package com.l8group.videoeditor.exceptions;

public class InvalidResizeParameterException extends RuntimeException {
    public InvalidResizeParameterException(String message) {
        super(message);
    }

    public InvalidResizeParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}