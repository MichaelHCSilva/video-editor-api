package com.l8group.videoeditor.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.services.VideoFileService;

@RestController
@RequestMapping("/api/videos")
public class VideoFileController {

    private final VideoFileService videoFileService;

    public VideoFileController(VideoFileService videoFileService) {
        this.videoFileService = videoFileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<UUID>> uploadVideos(@RequestParam("files") MultipartFile[] files) {
        try {
            List<UUID> videoIds = videoFileService.uploadVideos(files);
            return ResponseEntity.ok(videoIds);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
