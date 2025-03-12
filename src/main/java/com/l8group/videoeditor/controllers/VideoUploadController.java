package com.l8group.videoeditor.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dtos.*;
import com.l8group.videoeditor.requests.*;
import com.l8group.videoeditor.services.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/videos")
public class VideoUploadController {

    private final VideoFileService videoFileService;
    private final VideoCutService videoCutService;
    private final VideoResizeService videoResizeService;
    private final VideoOverlayService videoOverlayService;
    private final VideoConversionService videoConversionService;
    private final VideoBatchService videoBatchService;
    private final VideoDownloadService videoDownloadService;

    private static final Logger logger = LoggerFactory.getLogger(VideoUploadController.class);

    public VideoUploadController(VideoFileService videoFileService, VideoCutService videoCutService,
            VideoResizeService videoResizeService, VideoOverlayService videoOverlayService,
            VideoConversionService videoConversionService, VideoBatchService videoBatchService,
            VideoDownloadService videoDownloadService) {
        this.videoFileService = videoFileService;
        this.videoCutService = videoCutService;
        this.videoResizeService = videoResizeService;
        this.videoOverlayService = videoOverlayService;
        this.videoConversionService = videoConversionService;
        this.videoBatchService = videoBatchService;
        this.videoDownloadService = videoDownloadService;

    }

    @PostMapping("/upload")
    public ResponseEntity<VideoFileResponseDTO> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            VideoFileResponseDTO response = videoFileService.uploadVideo(file);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(null);
        }
    }

    @PostMapping("/edit/cut")
    public ResponseEntity<?> cutVideo(@RequestBody @Valid VideoCutRequest videoCutRequest,
            @RequestParam(value = "outputFilePath", required = false) String outputFilePath) {
        try {
            String outputFilePathResult = videoCutService.cutVideo(videoCutRequest, outputFilePath);
            return ResponseEntity.ok(outputFilePathResult);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar o corte do vídeo: " + e.getMessage());
        }
    }

    @PostMapping("/edit/resize")
    public ResponseEntity<?> resizeVideo(@Valid @RequestBody VideoResizeRequest request,
            @RequestParam(value = "outputFilePath", required = false) String outputFilePath) {
        try {
            String outputFilePathResult = videoResizeService.resizeVideo(request, outputFilePath);
            return ResponseEntity.ok(outputFilePathResult);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar o redimensionamento do vídeo: " + e.getMessage());
        }
    }

    @PostMapping("/edit/overlay-text")
    public ResponseEntity<?> overlayText(@RequestBody @Valid VideoOverlayRequest request,
            @RequestParam(value = "outputFilePath", required = false) String outputFilePath) {
        try {
            String outputFilePathResult = videoOverlayService.processOverlay(request, outputFilePath);
            return ResponseEntity.ok(outputFilePathResult);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar o overlay de texto: " + e.getMessage());
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertVideo(@Valid @RequestBody VideoConversionRequest request,
            @RequestParam(value = "outputFilePath", required = false) String outputFilePath) {
        logger.info("Recebida solicitação de conversão de vídeo: {}", request);
        try {
            String outputFilePathResult = videoConversionService.convertVideo(request, outputFilePath);
            logger.info("Conversão de vídeo concluída: {}", outputFilePathResult);
            return ResponseEntity.ok(outputFilePathResult);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar a conversão do vídeo: " + e.getMessage());
        }
    }

    @PostMapping("/batch-process")
    public ResponseEntity<?> processBatch(@Valid @RequestBody VideoBatchRequest request) {
        logger.info("Recebida solicitação de processamento em lote: {}", request);
        try {
            VideoBatchResponseDTO response = videoBatchService.processBatch(request);

            logger.info("Processamento em lote concluído. Resposta: {}", response);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar o lote de vídeos: " + e.getMessage());
        }
    }

    @GetMapping("/download/{batchProcessId}")
    public ResponseEntity<?> downloadVideo(@PathVariable UUID batchProcessId) {
        try {
            Resource fileResource = videoDownloadService.getProcessedVideo(batchProcessId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileResource.getFilename() + "\"")
                    .body(fileResource);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse);
        }
    }

    @GetMapping
    public List<VideoFileListDTO> listVideos() {
        return videoFileService.listAllVideos();
    }

}
