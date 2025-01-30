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

import com.l8group.videoeditor.dtos.VideoCutResponseDTO;
import com.l8group.videoeditor.dtos.VideoFileResponseDTO;
import com.l8group.videoeditor.dtos.VideoOverlayResponseDTO;
import com.l8group.videoeditor.dtos.VideoResizeResponseDTO;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.requests.VideoFileRequest;
import com.l8group.videoeditor.requests.VideoOverlayRequest;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.services.VideoCutService;
import com.l8group.videoeditor.services.VideoFileService;
import com.l8group.videoeditor.services.VideoOverlayService;
import com.l8group.videoeditor.services.VideoResizeService;
import com.l8group.videoeditor.services.VideoValidationService;

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
    private final VideoValidationService videoValidationService;

    public VideoFileController(VideoFileService videoFileService, VideoCutService videoCutService,
            VideoResizeService videoResizeService, VideoOverlayService videoOverlayService,
            VideoValidationService videoValidationService) {
        this.videoFileService = videoFileService;
        this.videoCutService = videoCutService;
        this.videoResizeService = videoResizeService;
        this.videoOverlayService = videoOverlayService;
        this.videoValidationService = videoValidationService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideos(@RequestParam(value = "files", required = false) MultipartFile[] files) {
        if (files == null) {
            return createErrorResponse("O parâmetro 'files' é obrigatório. Certifique-se de usar o nome correto.");
        }

        if (files.length == 0 || videoValidationService.allFilesAreEmpty(files)) {
            return createErrorResponse(
                    "Nenhum arquivo foi enviado. Por favor, selecione ao menos um arquivo de vídeo.");
        }

        List<VideoFileRequest> videoFileRequests = videoValidationService.createVideoFileRequests(files);
        if (videoFileRequests.isEmpty()) {
            return createErrorResponse(
                    "Todos os arquivos enviados são inválidos ou incompatíveis com os formatos aceitos.");
        }

        List<String> rejectedFiles = new ArrayList<>();
        List<UUID> processedFiles = videoFileService.uploadVideo(videoFileRequests, rejectedFiles);

        if (processedFiles.isEmpty()) {
            return createErrorResponse("Nenhum arquivo foi processado. Todos os arquivos enviados foram rejeitados.",
                    Map.of("rejected", rejectedFiles));
        }

        return createSuccessResponse(processedFiles, rejectedFiles);
    }

    private ResponseEntity<?> createErrorResponse(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<?> createErrorResponse(String message, Map<String, Object> additionalData) {
        Map<String, Object> response = new HashMap<>(additionalData);
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }

    private ResponseEntity<?> createSuccessResponse(List<UUID> processedFiles, List<String> rejectedFiles) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Upload concluído com sucesso.");
        response.put("processed", processedFiles);
        if (!rejectedFiles.isEmpty()) {
            response.put("rejected", rejectedFiles);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/videos")
    public ResponseEntity<List<VideoFileResponseDTO>> getAllVideos() {
        List<VideoFileResponseDTO> videos = videoFileService.getAllVideos();
        return ResponseEntity.ok(videos);
    }

    @PostMapping("/edit/cut")
    public ResponseEntity<?> cutVideo(@RequestBody @Valid VideoCutRequest videoCutRequest) {
        try {
            // Chama o serviço para cortar o vídeo
            VideoCutResponseDTO response = videoCutService.cutVideo(videoCutRequest);
            return ResponseEntity.ok(response); // Retorna a resposta com sucesso
        } catch (VideoProcessingException e) {
            // Tratamento específico para VideoProcessingException
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            // Tratamento específico para IOException
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Erro ao processar o vídeo. Detalhes: " + e.getMessage()));
        } catch (InterruptedException e) {
            // Tratamento específico para InterruptedException
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Processo interrompido durante o corte do vídeo."));
        } catch (Exception e) {
            // Tratamento genérico para outras exceções
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Ocorreu um erro inesperado ao processar o vídeo."));
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
}
