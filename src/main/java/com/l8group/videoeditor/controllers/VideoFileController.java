package com.l8group.videoeditor.controllers;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

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

import com.l8group.videoeditor.dtos.VideoConversionsDTO;
import com.l8group.videoeditor.dtos.VideoCutResponseDTO;
//import com.l8group.videoeditor.dtos.VideoCutResponseDTO;
import com.l8group.videoeditor.dtos.VideoFileResponseDTO;
import com.l8group.videoeditor.dtos.VideoOverlayResponseDTO;
import com.l8group.videoeditor.dtos.VideoResizeResponseDTO;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.requests.BatchProcessingRequest;
import com.l8group.videoeditor.requests.VideoConversionRequest;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.requests.VideoOverlayRequest;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.services.BatchProcessingService;
import com.l8group.videoeditor.services.VideoConversionService;
import com.l8group.videoeditor.services.VideoCutService;
import com.l8group.videoeditor.services.VideoFileService;
import com.l8group.videoeditor.services.VideoOverlayService;
import com.l8group.videoeditor.services.VideoResizeService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/videos")
public class VideoFileController {

    private static final Logger logger = LoggerFactory.getLogger(VideoFileController.class);

    private final VideoFileService videoFileService;
    private final VideoCutService videoCutService;
    private final VideoResizeService videoResizeService;
    private final VideoOverlayService videoOverlayService;

    private final VideoConversionService videoConversionService;
    private final BatchProcessingService batchProcessingService;

    public VideoFileController(VideoFileService videoFileService, VideoCutService videoCutService,
            VideoResizeService videoResizeService, VideoOverlayService videoOverlayService,
            VideoConversionService videoConversionService,
            BatchProcessingService batchProcessingService) {
        this.videoFileService = videoFileService;
        this.videoCutService = videoCutService;
        this.videoResizeService = videoResizeService;
        this.videoOverlayService = videoOverlayService;

        this.videoConversionService = videoConversionService;
        this.batchProcessingService = batchProcessingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideos(
            @Valid @RequestParam(value = "files", required = false) MultipartFile[] files) {
        return videoFileService.handleUpload(files);
    }

    @GetMapping
    public ResponseEntity<?> getAllVideos() {
        List<VideoFileResponseDTO> videoFileResponseDTOS = videoFileService.getAllVideos();

        if (videoFileResponseDTOS.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Nenhum vídeo encontrado"));
        }

        return ResponseEntity.ok(videoFileResponseDTOS);
    }

    @PostMapping("/edit/cut")
    public ResponseEntity<VideoCutResponseDTO> cutVideo(@RequestBody @Valid VideoCutRequest videoCutRequest) {
        try {
            VideoCutResponseDTO videoCutResponse = videoCutService.cutVideo(videoCutRequest);

            return ResponseEntity.ok(videoCutResponse);
        } catch (VideoProcessingException e) {
            return ResponseEntity.badRequest().body(new VideoCutResponseDTO("", e.getMessage(), ZonedDateTime.now()));
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(new VideoCutResponseDTO("", "Erro ao processar o vídeo. Detalhes: " + e.getMessage(),
                            ZonedDateTime.now()));
        } catch (InterruptedException e) {
            return ResponseEntity.status(500)
                    .body(new VideoCutResponseDTO("", "Processo interrompido durante o corte do vídeo.",
                            ZonedDateTime.now()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new VideoCutResponseDTO("", "Ocorreu um erro inesperado ao processar o vídeo.",
                            ZonedDateTime.now()));
        }
    }

    @PostMapping("/edit/resize")
    public ResponseEntity<?> resizeVideo(@RequestBody @Valid VideoResizeRequest videoResizeRequest) {
        try {
            VideoResizeResponseDTO resizedVideo = videoResizeService.resizeVideo(videoResizeRequest);
            return ResponseEntity.ok(resizedVideo);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Erro interno do servidor."));
        }
    }

    @PostMapping("/edit/overlay-text")
    public ResponseEntity<?> overlayTextOnVideo(@RequestBody @Valid VideoOverlayRequest overlayRequest) {
        try {
            VideoOverlayResponseDTO responseDTO = videoOverlayService.createOverlay(overlayRequest);
            return ResponseEntity.ok(responseDTO);
        } catch (EntityNotFoundException e) {
            logger.error("Erro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.error("Erro de validação: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro inesperado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ocorreu um erro inesperado ao aplicar a sobreposição de texto."));
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> convertVideo(@Valid @RequestBody VideoConversionRequest request) {
        try {
            VideoConversionsDTO response = videoConversionService.convertVideo(request);
            return ResponseEntity.ok(Map.of("message", "Conversão realizada com sucesso", "data", response));
        } catch (IllegalArgumentException | VideoProcessingException e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Erro ao processar a conversão.", "details", e.getMessage()));
        }
    }

    @PostMapping("/batch-process")
    public ResponseEntity<String> processBatch(@Valid @RequestBody BatchProcessingRequest batchRequest) {
        logger.info("Recebida solicitação para processamento em lote de vídeos. IDs: {}", batchRequest.getVideoIds());

        try {
            batchProcessingService.processBatch(batchRequest);
            return ResponseEntity.ok("Processamento em lote iniciado com sucesso.");
        } catch (IOException | InterruptedException e) {
            logger.error("Erro ao processar vídeos em lote: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Erro ao processar vídeos em lote.");
        }
    }

}
