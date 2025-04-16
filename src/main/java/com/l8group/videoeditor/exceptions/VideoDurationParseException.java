package com.l8group.videoeditor.exceptions;

public class VideoDurationParseException extends RuntimeException {
    public VideoDurationParseException(String message) {
        super(message);
    }

    public VideoDurationParseException(String message, Throwable cause) {
        super(message, cause);
    }
}