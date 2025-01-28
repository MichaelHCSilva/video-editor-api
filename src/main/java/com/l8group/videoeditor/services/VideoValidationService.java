package com.l8group.videoeditor.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.requests.VideoFileRequest;

@Service
public class VideoValidationService {

    public boolean isSupportedFormat(String fileFormat) {
        return fileFormat.equalsIgnoreCase("mp4")
                || fileFormat.equalsIgnoreCase("avi")
                || fileFormat.equalsIgnoreCase("mov");
    }

    public String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("O nome do arquivo é inválido ou não possui uma extensão válida.");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    public boolean isValidVideoContent(MultipartFile file) {
        try {
            return file.getSize() > 0;
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao validar o conteúdo do vídeo.");
        }
    }

    public boolean allFilesAreEmpty(MultipartFile[] files) {
        return Arrays.stream(files).allMatch(file -> file == null || file.isEmpty());
    }

    public List<VideoFileRequest> createVideoFileRequests(MultipartFile[] files) {
        List<VideoFileRequest> videoFileRequests = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                videoFileRequests.add(new VideoFileRequest(file));
            }
        }
        return videoFileRequests;
    }
}
