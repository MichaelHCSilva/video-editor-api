package com.l8group.videoeditor.dtos;

import java.util.UUID;

public class VideoUploadResponseDTO {

    private final UUID id;
    private final String message;

    public VideoUploadResponseDTO(UUID id, String message) {
        this.id = id;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}
