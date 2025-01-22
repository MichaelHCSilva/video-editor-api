package com.l8group.videoeditor.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dtos.VideoCutDTO;
import com.l8group.videoeditor.dtos.VideoFileDTO;
import com.l8group.videoeditor.models.VideoCut;
import com.l8group.videoeditor.services.VideoCutService;
import com.l8group.videoeditor.services.VideoFileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/videos")
public class VideoFileController {

    private static final Logger logger = LoggerFactory.getLogger(VideoFileController.class);

    private final VideoFileService videoFileService;
    private final VideoCutService videoCutService;

    public VideoFileController(VideoFileService videoFileService, VideoCutService videoCutService) {
        this.videoFileService = videoFileService;
        this.videoCutService = videoCutService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideos(@RequestParam(value = "files", required = false) MultipartFile[] files) {
        if (files == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "O parâmetro 'files' é obrigatório. Certifique-se de usar o nome correto."));
        }

        if (files.length == 0 || allFilesAreEmpty(files)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Nenhum arquivo foi enviado. Por favor, selecione ao menos um arquivo de vídeo."));
        }

        List<String> rejectedFiles = new ArrayList<>();
        List<UUID> processedFiles = videoFileService.uploadVideo(files, rejectedFiles);

        if (processedFiles.isEmpty() && rejectedFiles.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Nenhum arquivo válido foi enviado. Por favor, tente novamente com arquivos de vídeo."));
        }

        if (processedFiles.isEmpty() && !rejectedFiles.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Nenhum arquivo foi processado. Todos os arquivos enviados foram rejeitados.",
                            "rejected", rejectedFiles));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Upload concluído com sucesso.");
        response.put("processed", processedFiles);
        if (!rejectedFiles.isEmpty()) {
            response.put("rejected", rejectedFiles);
        }

        return ResponseEntity.ok().body(response);
    }

    private boolean allFilesAreEmpty(MultipartFile[] files) {
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @GetMapping
    public ResponseEntity<?> getVideos() {
        List<VideoFileDTO> videos = videoFileService.getAllVideos();

        if (videos.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Nenhum vídeo encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(response);
        }

        return ResponseEntity.ok(videos);
    }

    @PostMapping("/edit/cut")
    public ResponseEntity<?> cutVideo(@RequestBody @Valid VideoCutDTO videoCutDTO) {
        try {
            VideoCut videoCut = videoCutService.cutVideo(videoCutDTO);
            return ResponseEntity.ok(videoCut);
        } catch (IllegalArgumentException e) {
            logger.error("Erro de validação: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            logger.error("Erro ao cortar o vídeo: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erro ao cortar o vídeo. Detalhes: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Erro inesperado: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ocorreu um erro inesperado ao processar o vídeo.");
        }
    }
}
