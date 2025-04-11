package com.l8group.videoeditor.controllers;

//import java.io.IOException;
import java.util.*;

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
import com.l8group.videoeditor.responses.ErrorResponse;
import com.l8group.videoeditor.responses.SuccessResponse;
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
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") List<MultipartFile> files) {
        List<VideoFileResponseDTO> successList = new ArrayList<>();
        List<Map<String, String>> errorList = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                VideoFileResponseDTO response = videoFileService.uploadVideo(file);
                successList.add(response);
            } catch (Exception e) {
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

    @PostMapping("/edit/cut")
    public ResponseEntity<?> cutVideo(@Valid @RequestBody VideoCutRequest request,
            @RequestParam(value = "outputFilePath", required = false) String outputFilePath) {
        try {
            String path = videoCutService.cutVideo(request, outputFilePath);
            return ResponseEntity.ok(new SuccessResponse("Vídeo cortado com sucesso: " + path));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Erro ao cortar vídeo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erro ao processar o corte do vídeo: " + e.getMessage()));
        }
    }

    @PostMapping("/edit/resize")
    public ResponseEntity<?> resizeVideo(@Valid @RequestBody VideoResizeRequest request,
            @RequestParam(value = "outputFilePath", required = false) String outputFilePath) {
        try {
            String path = videoResizeService.resizeVideo(request, outputFilePath);
            return ResponseEntity.ok(new SuccessResponse("Vídeo redimensionado com sucesso: " + path));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Erro ao redimensionar vídeo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erro ao processar o redimensionamento do vídeo: " + e.getMessage()));
        }
    }

    @PostMapping("/edit/overlay-text")
    public ResponseEntity<?> overlayText(@Valid @RequestBody VideoOverlayRequest request,
            @RequestParam(value = "outputFilePath", required = false) String outputFilePath) {
        try {
            String path = videoOverlayService.processOverlay(request, outputFilePath);
            return ResponseEntity.ok(new SuccessResponse("Texto aplicado com sucesso: " + path));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Erro ao aplicar overlay", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erro ao processar o overlay de texto: " + e.getMessage()));
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertVideo(@Valid @RequestBody VideoConversionRequest request,
            @RequestParam(value = "outputFilePath", required = false) String outputFilePath) {
        logger.info("Recebida solicitação de conversão de vídeo: {}", request);
        try {
            String path = videoConversionService.convertVideo(request, outputFilePath);
            return ResponseEntity.ok(new SuccessResponse("Conversão realizada com sucesso: " + path));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Erro ao converter vídeo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erro ao processar a conversão do vídeo: " + e.getMessage()));
        }
    }

    @PostMapping("/batch-process")
    public ResponseEntity<?> processBatch(@Valid @RequestBody VideoBatchRequest request) {
        logger.info("Recebida solicitação de processamento em lote: {}", request);
        try {
            VideoBatchResponseDTO response = videoBatchService.processBatch(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Erro no processamento em lote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erro ao processar o lote de vídeos: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{batchProcessId}")
    public ResponseEntity<?> downloadVideo(@PathVariable UUID batchProcessId) {
        try {
            Resource resource = videoDownloadService.getProcessedVideo(batchProcessId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Erro ao fazer download do vídeo", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Vídeo não encontrado: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<VideoFileListDTO>> listVideos() {
        List<VideoFileListDTO> videos = videoFileService.listAllVideos();
        return ResponseEntity.ok(videos);
    }
}
