package com.l8group.videoeditor.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
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
import com.l8group.videoeditor.requests.VideoBatchRequest;
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
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam(value = "file", required = false) List<MultipartFile> files) {

        List<VideoFileResponseDTO> successList = new ArrayList<>();
        List<Map<String, String>> errorList = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            errorList.add(Map.of("error", "O campo 'file' é obrigatório e deve conter pelo menos um arquivo."));
        } else {
            for (MultipartFile file : files) {
                try {
                    if (file == null || file.isEmpty()) {
                        throw new IllegalArgumentException("O arquivo enviado está vazio.");
                    }

                    if (!file.getContentType().startsWith("video/")) {
                        throw new IllegalArgumentException(
                                "O arquivo enviado não é um vídeo válido e não pode ser processado.");
                    }

                    VideoFileResponseDTO response = videoFileService.uploadVideo(file);
                    successList.add(response);

                } catch (Exception e) {
                    Map<String, String> errorMap = new HashMap<>();

                    String fileName = (file != null && file.getOriginalFilename() != null
                            && !file.getOriginalFilename().isBlank())
                                    ? file.getOriginalFilename()
                                    : null;

                    if (fileName != null) {
                        errorMap.put("fileName", fileName);
                    }

                    errorMap.put("error", e.getMessage());
                    errorList.add(errorMap);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (!successList.isEmpty())
            result.put("success", successList);
        if (!errorList.isEmpty())
            result.put("errors", errorList);

        HttpStatus status = successList.isEmpty() ? HttpStatus.BAD_REQUEST : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result);
    }

    @PostMapping("/batch-process")
    public ResponseEntity<?> processBatch(@Valid @RequestBody VideoBatchRequest request) throws IOException {
        log.info("Recebida solicitação de processamento em lote: {}", request);
        VideoBatchResponseDTO response = videoBatchService.processBatch(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{batchProcessId}")
    public ResponseEntity<?> downloadVideo(@PathVariable String batchProcessId) {
        log.info("Requisição de download para o vídeo com ID: {}", batchProcessId);
        return videoDownloadService.downloadVideoStreamFromS3(batchProcessId);
    }

    @GetMapping
    public ResponseEntity<List<VideoFileListDTO>> listVideos() {
        List<VideoFileListDTO> videos = videoFileService.listAllVideos();
        return ResponseEntity.ok(videos);
    }
}