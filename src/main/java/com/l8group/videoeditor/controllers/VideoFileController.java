package com.l8group.videoeditor.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> uploadVideos(@RequestParam(value = "files", required = false) MultipartFile[] files) {
        // Caso o parâmetro 'files' não tenha sido enviado
        if (files == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "O parâmetro 'files' é obrigatório. Certifique-se de usar o nome correto."));
        }

        // Caso o parâmetro 'files' esteja presente, mas vazio
        if (files.length == 0 || allFilesAreEmpty(files)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Nenhum arquivo foi enviado. Por favor, selecione ao menos um arquivo de vídeo."));
        }

        // Inicializa as listas de arquivos processados e rejeitados
        List<String> rejectedFiles = new ArrayList<>();
        List<UUID> processedFiles = videoFileService.uploadVideos(files, rejectedFiles);

        // Verifica se nenhum arquivo foi processado e nenhum foi explicitamente
        // rejeitado
        if (processedFiles.isEmpty() && rejectedFiles.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Nenhum arquivo válido foi enviado. Por favor, tente novamente com arquivos de vídeo."));
        }

        // Caso todos os arquivos tenham sido rejeitados
        if (processedFiles.isEmpty() && !rejectedFiles.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message", "Nenhum arquivo foi processado. Todos os arquivos enviados foram rejeitados.",
                            "rejected", rejectedFiles));
        }

        // Constrói a resposta com ou sem 'rejectedFiles', dependendo do conteúdo
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Upload concluído com sucesso.");
        response.put("processed", processedFiles);
        if (!rejectedFiles.isEmpty()) {
            response.put("rejected", rejectedFiles);
        }

        return ResponseEntity.ok().body(response);
    }

    // Método auxiliar para verificar se todos os arquivos estão vazios
    private boolean allFilesAreEmpty(MultipartFile[] files) {
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                return false; // Encontrou pelo menos um arquivo válido
            }
        }
        return true; // Todos os arquivos estão vazios ou nulos
    }

    @GetMapping
    public ResponseEntity<?> getVideos() {
        List<VideoFileDTO> videos = videoFileService.getAllVideos();

        if (videos.isEmpty()) {
            // Retorna uma resposta com status 404 e a mensagem personalizada em formato
            // JSON
            Map<String, String> response = new HashMap<>();
            response.put("message", "Nenhum vídeo encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(response);
        }

        return ResponseEntity.ok(videos);
    }

}
