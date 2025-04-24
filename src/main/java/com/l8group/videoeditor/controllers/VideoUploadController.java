package com.l8group.videoeditor.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dtos.VideoBatchResponseDTO;
import com.l8group.videoeditor.dtos.VideoFileListDTO;
import com.l8group.videoeditor.dtos.VideoFileResponseDTO;
import com.l8group.videoeditor.exceptions.InvalidCutTimeException;
import com.l8group.videoeditor.requests.VideoBatchRequest;
import com.l8group.videoeditor.responses.ErrorResponse;
import com.l8group.videoeditor.services.VideoBatchService;
import com.l8group.videoeditor.services.VideoDownloadService;
import com.l8group.videoeditor.services.VideoFileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoUploadController {

    private final VideoFileService videoFileService;
    private final VideoBatchService videoBatchService;
    private final VideoDownloadService videoDownloadService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") List<MultipartFile> files) {
        List<VideoFileResponseDTO> successList = new ArrayList<>();
        List<Map<String, String>> errorList = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                VideoFileResponseDTO response = videoFileService.uploadVideo(file);
                successList.add(response);
            } catch (Exception e) {
                log.error("Erro ao fazer upload do vídeo '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("fileName", file.getOriginalFilename());
                errorMap.put("error", e.getMessage());
                errorList.add(errorMap);
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (!successList.isEmpty())
            result.put("success", successList);
        if (!errorList.isEmpty())
            result.put("errors", errorList);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch-process")
    public ResponseEntity<?> processBatch(@Valid @RequestBody VideoBatchRequest request) {
        log.info("Recebida solicitação de processamento em lote: {}", request);
        try {
            VideoBatchResponseDTO response = videoBatchService.processBatch(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao processar vídeo em lote: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/download/{batchProcessId}")
    public ResponseEntity<?> downloadVideo(@PathVariable UUID batchProcessId) {
        try {
            Resource resource = videoDownloadService.getProcessedVideo(batchProcessId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Erro ao fazer download do vídeo com ID {}", batchProcessId, e);
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("Vídeo não encontrado: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<VideoFileListDTO>> listVideos() {
        List<VideoFileListDTO> videos = videoFileService.listAllVideos();
        return ResponseEntity.ok(videos);
    }
}