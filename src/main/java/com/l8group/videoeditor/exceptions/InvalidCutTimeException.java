package com.l8group.videoeditor.exceptions;

public class InvalidCutTimeException extends RuntimeException {

    public InvalidCutTimeException(String message) {
        super(message);
    }

    public InvalidCutTimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
