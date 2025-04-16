package com.l8group.videoeditor.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Ou outro status HTTP apropriado
public class VideoProcessingException extends RuntimeException {

    public VideoProcessingException(String message) {
        super(message);
    }

    public VideoProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}