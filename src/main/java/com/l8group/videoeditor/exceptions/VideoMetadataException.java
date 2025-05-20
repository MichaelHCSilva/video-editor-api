package com.l8group.videoeditor.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) 
public class VideoMetadataException extends RuntimeException {
    public VideoMetadataException(String message) {
        super(message);
    }

    public VideoMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}