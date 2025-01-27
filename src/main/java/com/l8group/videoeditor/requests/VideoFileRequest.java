package com.l8group.videoeditor.requests;

import org.springframework.web.multipart.MultipartFile;

public class VideoFileRequest {
    
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
