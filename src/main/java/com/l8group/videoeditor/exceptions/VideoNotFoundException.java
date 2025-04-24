package com.l8group.videoeditor.exceptions;

import java.util.UUID;

public class VideoNotFoundException extends RuntimeException {

    public VideoNotFoundException(UUID id) {
        super("Vídeo não encontrado para o ID: " + id);
    }

    public VideoNotFoundException(String message) {
        super(message);
    }
}