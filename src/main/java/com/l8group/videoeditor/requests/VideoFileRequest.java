package com.l8group.videoeditor.requests;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

public class VideoFileRequest {

    @NotNull(message = "O arquivo de vídeo é obrigatório.")
    private MultipartFile file;

    public VideoFileRequest(MultipartFile file) {
        this.file = file;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
