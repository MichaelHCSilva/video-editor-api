package com.l8group.videoeditor.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dtos.VideoFileDTO;
import com.l8group.videoeditor.services.VideoFileService;

@RestController
@RequestMapping("/api/videos")
public class VideoFileController {

    private final VideoFileService videoFileService;

    public VideoFileController(VideoFileService videoFileService) {
        this.videoFileService = videoFileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideos(@RequestParam("files") MultipartFile[] files) {
        List<String> rejectedFiles = new ArrayList<>();
        List<UUID> processedFiles = videoFileService.uploadVideos(files, rejectedFiles);

        // Retorna informações de arquivos processados e rejeitados
        return ResponseEntity.ok().body(
                Map.of(
                        "processed", processedFiles,
                        "rejected", rejectedFiles));
    }

    @GetMapping
    public ResponseEntity<List<VideoFileDTO>> getVideos() {
        List<VideoFileDTO> videos = videoFileService.getAllVideos();
        return ResponseEntity.ok(videos);
    }
}
