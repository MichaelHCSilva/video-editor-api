package com.l8group.videoeditor.exceptions;

public class FileAlreadyExistsInS3Exception extends RuntimeException {
    public FileAlreadyExistsInS3Exception(String message) {
        super(message);
    }
}