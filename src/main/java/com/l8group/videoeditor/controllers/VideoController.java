package com.l8group.videoeditor.controllers;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dto.VideoFileDTO;
import com.l8group.videoeditor.services.VideoUploadService;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoUploadService videoUploadService;

    @PostMapping("/upload")
    public ResponseEntity<UUID> uploadVideo(@RequestParam("file") MultipartFile file) {
        logger.info("Recebendo solicitação de upload de vídeo...");

        try {
            UUID videoId = videoUploadService.uploadVideo(file);
            logger.info("Upload concluído com sucesso. ID do vídeo: {}", videoId);

            return ResponseEntity.ok(videoId);
        } catch (IllegalArgumentException e) {
            logger.warn("Erro no upload de vídeo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Erro inesperado ao fazer upload do vídeo.", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<VideoFileDTO>> listarVideos() {
        List<VideoFileDTO> videoFileDTOs = videoUploadService.listarVideos();
        return ResponseEntity.ok(videoFileDTOs);
    }
}
