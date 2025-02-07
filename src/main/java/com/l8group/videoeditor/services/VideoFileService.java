package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dtos.VideoFileResponseDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoFileRequest;
import com.l8group.videoeditor.utils.VideoDurationUtils;

@Service
public class VideoFileService {

    private static final Logger logger = LoggerFactory.getLogger(VideoFileService.class);

    private final VideoFileRepository videoFileRepository;
    private final VideoValidationService validationService;
    private final VideoDurationUtils videoDurationUtils;
    private final TaskExecutor taskExecutor;

    private final String uploadDir = System.getProperty("user.dir") + "/videos/";

    @Autowired
    public VideoFileService(VideoFileRepository videoFileRepository, VideoValidationService validationService,
            VideoDurationUtils videoDurationUtils, TaskExecutor taskExecutor) {
        this.videoFileRepository = videoFileRepository;
        this.validationService = validationService;
        this.videoDurationUtils = videoDurationUtils;
        this.taskExecutor = taskExecutor;
    }

    public ResponseEntity<?> handleUpload(MultipartFile[] files) {
        if (files == null || files.length == 0 || validationService.allFilesAreEmpty(files)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nenhum arquivo válido enviado."));
        }
    
        List<VideoFileRequest> videoFileRequests = validationService.createVideoFileRequests(files);
        if (videoFileRequests.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Todos os arquivos são inválidos."));
        }
    
        List<UUID> processedFiles = uploadVideo(videoFileRequests).join();
    
        List<String> rejectedFiles = videoFileRequests.stream()
                .filter(request -> !validationService.isSupportedFormat(validationService.getFileExtension(request.getFile().getOriginalFilename())))
                .map(request -> request.getFile().getOriginalFilename() + " (Formato inválido)")
                .collect(Collectors.toList());
    
        Map<String, Object> response = new HashMap<>();
        
        if (rejectedFiles.isEmpty()) {
            if (!processedFiles.isEmpty()) {
                response.put("message", "Upload concluído.");
                response.put("processed_ids", processedFiles);
            }
        } else {
            if (!processedFiles.isEmpty()) {
                response.put("message", "Upload concluído.");
                response.put("processed_ids", processedFiles);
            } else {
                response.put("message", "Nenhum arquivo válido enviado.");
            }
            response.put("rejected_files", rejectedFiles);
        }
    
        return ResponseEntity.ok(response);
    }
    
    public CompletableFuture<List<UUID>> uploadVideo(List<VideoFileRequest> videoFileRequests) {
        ConcurrentLinkedQueue<String> rejectedFiles = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<UUID> processedFiles = new ConcurrentLinkedQueue<>();

        List<CompletableFuture<Void>> futures = videoFileRequests.stream()
                .map(request -> CompletableFuture
                        .runAsync(() -> processVideoFile(request, rejectedFiles, processedFiles), taskExecutor)
                        .exceptionally(ex -> {
                            logger.error("Erro ao processar vídeo {}: {}", request.getFile().getOriginalFilename(),
                                    ex.getMessage());
                            rejectedFiles.add(request.getFile().getOriginalFilename() + " (Erro inesperado)");
                            return null;
                        }))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> new ArrayList<>(processedFiles));

    }

    private void processVideoFile(VideoFileRequest request, ConcurrentLinkedQueue<String> rejectedFiles,
            ConcurrentLinkedQueue<UUID> processedFiles) {
        MultipartFile file = request.getFile();
        String fileFormat = validationService.getFileExtension(file.getOriginalFilename());

        if (!validationService.isSupportedFormat(fileFormat)) {
            rejectedFiles.add(file.getOriginalFilename() + " (Formato inválido: " + fileFormat + ")");
            return;
        }

        try {
            String filePath = saveFile(file, fileFormat);
            VideoFile videoFile = createVideoFile(file, fileFormat, filePath);
            saveVideoFile(videoFile);

            processedFiles.add(videoFile.getId());

            videoDurationUtils.getVideoDurationAsync(filePath).thenAccept(duration -> {
                videoFile.setDuration(duration.getSeconds());
                videoFileRepository.save(videoFile);
                logger.info("Duração do vídeo {}: {} segundos", videoFile.getFileName(), duration.getSeconds());
            });

            logger.info("Arquivo processado: {}", file.getOriginalFilename());
        } catch (IOException e) {
            logger.error("Erro ao salvar/processar {}: {}", file.getOriginalFilename(), e.getMessage());
            rejectedFiles.add(file.getOriginalFilename() + " (Erro ao salvar/processar)");
        }
    }

    private String saveFile(MultipartFile file, String fileFormat) throws IOException {
        String uniqueFileName = generateFileName(file, fileFormat);
        File directory = new File(uploadDir);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        String filePath = uploadDir + uniqueFileName;
        file.transferTo(new File(filePath));

        return filePath;
    }

    private String generateFileName(MultipartFile file, String targetFormat) {
        if (file == null) {
            throw new IllegalArgumentException("O arquivo não pode ser nulo.");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do arquivo não pode ser nulo ou vazio.");
        }

        String baseName = originalFileName.replaceAll("\\.[^.]+$", "");
        String timestamp = new SimpleDateFormat("yyyyMMdd").format(new java.util.Date());

        String videoId = UUID.randomUUID().toString();

        String fileName = baseName + "_" + videoId.substring(0, 8) + "_" + timestamp + "." + targetFormat;
        logger.info("Nome do arquivo gerado: {}", fileName);

        return fileName;
    }

    private VideoFile createVideoFile(MultipartFile file, String fileFormat, String filePath) {
        VideoFile videoFile = new VideoFile();
        videoFile.setFileName(file.getOriginalFilename());
        videoFile.setFileSize(file.getSize());
        videoFile.setFileFormat(fileFormat);
        videoFile.setCreatedAt(ZonedDateTime.now());
        videoFile.setUploadedAt(ZonedDateTime.now());
        videoFile.setStatus(VideoStatus.PROCESSING);
        videoFile.setFilePath(filePath);
        videoFile.setDuration(0L);
        return videoFile;
    }

    @Transactional
    private void saveVideoFile(VideoFile videoFile) {
        videoFileRepository.save(videoFile);
    }

    public List<VideoFileResponseDTO> getAllVideos() {
        return videoFileRepository.findAllVideos();
    }
}
