package com.l8group.videoeditor.controllers;

import java.io.IOException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;

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
import com.l8group.videoeditor.dtos.VideoConversionsDTO;
import com.l8group.videoeditor.dtos.VideoCutResponseDTO;
import com.l8group.videoeditor.dtos.VideoFileResponseDTO;
import com.l8group.videoeditor.dtos.VideoResizeResponseDTO;
import com.l8group.videoeditor.dtos.VideoUploadResponseDTO;
import com.l8group.videoeditor.requests.VideoBatchRequest;
import com.l8group.videoeditor.requests.VideoConversionRequest;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.requests.VideoFileRequest;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.services.VideoBatchService;
import com.l8group.videoeditor.services.VideoConversionService;
import com.l8group.videoeditor.services.VideoCutService;
import com.l8group.videoeditor.services.VideoFileService;
import com.l8group.videoeditor.services.VideoResizeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/videos")
public class VideoFileController {

    // private static final Logger logger =
    // LoggerFactory.getLogger(VideoFileController.class);


    private final VideoFileService videoFileService;
    private final VideoCutService videoCutService;
    private final VideoResizeService videoResizeService;
    // private final VideoFileRepository videoFileRepository;

    private final VideoConversionService videoConversionService;
    private final VideoBatchService videoBatchService;

    public VideoFileController(VideoFileService videoFileService, VideoCutService videoCutService,
            VideoResizeService videoResizeService,
            VideoConversionService videoConversionService,
            VideoBatchService videoBatchService) {
        this.videoFileService = videoFileService;
        this.videoCutService = videoCutService;
        this.videoResizeService = videoResizeService;

        this.videoConversionService = videoConversionService;
        this.videoBatchService = videoBatchService;
    }

    @PostMapping("/upload")
    public ResponseEntity<VideoUploadResponseDTO> uploadVideo(@RequestParam("file") MultipartFile file) {
        VideoFileRequest request = new VideoFileRequest(file);
        VideoUploadResponseDTO response = videoFileService.uploadVideo(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<VideoFileResponseDTO>> listAllVideos() {
        List<VideoFileResponseDTO> videos = videoFileService.listAllVideos();
        return ResponseEntity.ok(videos);
    }

    @PostMapping("/edit/cut")
    public ResponseEntity<?> cutVideo(@RequestBody @Valid VideoCutRequest videoCutRequest) {
        try {
            VideoCutResponseDTO response = videoCutService.cutVideo(videoCutRequest);
            return ResponseEntity.ok(response);
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.internalServerError().body("Erro ao cortar o vídeo: " + e.getMessage());
        }
    }

    @PostMapping("/edit/resize")
    public ResponseEntity<VideoResizeResponseDTO> resizeVideo(
            @RequestBody @Valid VideoResizeRequest videoResizeRequest) {
        try {
            VideoResizeResponseDTO response = videoResizeService.resizeVideo(videoResizeRequest);
            return ResponseEntity.ok(response);
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.internalServerError()
                    .body(new VideoResizeResponseDTO("", "Erro ao processar o vídeo.", ZonedDateTime.now()));
        }
    }

    /*
     * @PostMapping("/edit/overlay-text")
     * public ResponseEntity<?> overlayTextOnVideo(@RequestBody @Valid
     * VideoOverlayRequest overlayRequest) {
     * try {
     * VideoOverlayResponseDTO responseDTO =
     * videoOverlayService.createOverlay(overlayRequest);
     * return ResponseEntity.ok(responseDTO);
     * } catch (EntityNotFoundException e) {
     * logger.error("Erro: {}", e.getMessage());
     * return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message",
     * e.getMessage()));
     * } catch (IllegalArgumentException e) {
     * logger.error("Erro de validação: {}", e.getMessage());
     * return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
     * } catch (Exception e) {
     * logger.error("Erro inesperado: {}", e.getMessage());
     * return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
     * .body(Map.of("message",
     * "Ocorreu um erro inesperado ao aplicar a sobreposição de texto."));
     * }
     * }
     */

    @PostMapping("/convert")
    public ResponseEntity<VideoConversionsDTO> convertVideo(@RequestBody @Valid VideoConversionRequest request) {
        try {
            VideoConversionsDTO response = videoConversionService.convertVideo(request);
            return ResponseEntity.ok(response);
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.internalServerError()
                    .body(new VideoConversionsDTO("", "", "", ZonedDateTime.now()));
        }
    }

    @PostMapping("/batch-process")
    public ResponseEntity<?> processBatch(@RequestBody @Valid VideoBatchRequest batchRequest) {
        try {
            VideoBatchResponseDTO response = videoBatchService.processBatch(batchRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao processar lote: " + e.getMessage());
        }
    }

    @GetMapping("/download/{videoId}")
    public ResponseEntity<Resource> downloadProcessedVideo(@PathVariable UUID videoId) {
        return videoFileService.downloadProcessedVideo(videoId);
    }

}
