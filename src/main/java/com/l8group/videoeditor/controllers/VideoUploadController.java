package com.l8group.videoeditor.controllers;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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
import com.l8group.videoeditor.dtos.VideoFileResponseDTO;
import com.l8group.videoeditor.requests.VideoBatchRequest;
import com.l8group.videoeditor.requests.VideoConversionRequest;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.requests.VideoOverlayRequest;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.services.VideoBatchService;
import com.l8group.videoeditor.services.VideoConversionService;
import com.l8group.videoeditor.services.VideoCutService;
import com.l8group.videoeditor.services.VideoDownloadService;
import com.l8group.videoeditor.services.VideoFileService;
import com.l8group.videoeditor.services.VideoOverlayService;
import com.l8group.videoeditor.services.VideoResizeService;

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
                    .body(null); // Retorna null para evitar exposição de detalhes no erro
        }
    }

    @PostMapping("/edit/cut")
    public ResponseEntity<?> cutVideo(@RequestBody @Valid VideoCutRequest videoCutRequest,
            @RequestParam(value = "outputFilePath", required = false) String outputFilePath) {
        try {
            String outputFilePathResult = videoCutService.cutVideo(videoCutRequest, outputFilePath); // Agora retorna
                                                                                                     // String
            return ResponseEntity.ok(outputFilePathResult); // Retorna o caminho do arquivo
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
            String outputFilePathResult = videoResizeService.resizeVideo(request, outputFilePath); // Agora retorna
                                                                                                   // String
            return ResponseEntity.ok(outputFilePathResult); // Retorna o caminho do arquivo
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
            String outputFilePathResult = videoOverlayService.processOverlay(request, outputFilePath); // Agora retorna
                                                                                                       // String
            return ResponseEntity.ok(outputFilePathResult); // Retorna o caminho do arquivo
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
            String outputFilePathResult = videoConversionService.convertVideo(request, outputFilePath); // Agora retorna
                                                                                                        // String
            logger.info("Conversão de vídeo concluída: {}", outputFilePathResult);
            return ResponseEntity.ok(outputFilePathResult); // Retorna o caminho do arquivo
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
public ResponseEntity<Resource> downloadProcessedVideo(@PathVariable UUID batchProcessId) {
    Resource file = videoDownloadService.getProcessedVideo(batchProcessId);

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
            .body(file);
}

}
