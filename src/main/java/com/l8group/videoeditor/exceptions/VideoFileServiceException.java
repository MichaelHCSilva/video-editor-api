package com.l8group.videoeditor.exceptions;

public class VideoFileServiceException extends RuntimeException {

    public VideoFileServiceException(String message) {
        super(message);
    }

    public VideoFileServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
