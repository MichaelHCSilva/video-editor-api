package com.l8group.videoeditor.controllers;

import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoFileRepository videoFileRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file) {
        logger.info("Iniciando upload de vídeo...");

        if (file.isEmpty()) {
            logger.warn("Nenhum arquivo enviado na requisição.");
            return ResponseEntity.badRequest().body("Nenhum arquivo enviado.");
        }

        try {
            String contentType = file.getContentType();
            logger.info("Tipo de conteúdo recebido: {}", contentType);  

            if (!isValidVideoFormat(contentType)) {
                logger.warn("Formato de vídeo inválido: {}", contentType);
                return ResponseEntity.badRequest().body("Formato de vídeo inválido! Envie MP4, AVI ou MOV.");
            }

            logger.info("Criando entidade VideoFile para salvar no banco...");
            VideoFile videoFile = new VideoFile();
            videoFile.setFileName(file.getOriginalFilename());
            videoFile.setFileSize(file.getSize());
            videoFile.setFileFormat(contentType);
            videoFile.setUploadedAt(ZonedDateTime.now());

            VideoFile savedFile = videoFileRepository.save(videoFile);
            logger.info("Arquivo salvo com sucesso no banco. ID: {}", savedFile.getId());

            return ResponseEntity.ok(savedFile.getId());
        } catch (Exception e) {
            logger.error("Erro ao processar o upload do vídeo", e);  
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao enviar o vídeo: " + e.getMessage());
        }
    }

    private boolean isValidVideoFormat(String contentType) {
        logger.debug("Validando o formato de vídeo: {}", contentType);
        return contentType != null &&
               (contentType.equals("video/mp4") ||
                contentType.equals("video/x-msvideo") || 
                contentType.equals("video/quicktime"));  
    }
}
