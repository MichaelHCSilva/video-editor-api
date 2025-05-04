package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.l8group.videoeditor.dtos.VideoFileListDTO;
import com.l8group.videoeditor.dtos.VideoFileResponseDTO;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.NoVideosFoundException;
import com.l8group.videoeditor.metrics.VideoFileServiceMetrics;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.rabbit.producer.VideoProcessingProducer;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.utils.VideoFileStorageUtils;
import com.l8group.videoeditor.utils.VideoDurationUtils;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.validation.VideoValidationExecutor;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoFileService {

    @Value("${video.upload.dir}")
    private String STORAGE_DIR;

    private final VideoFileRepository videoFileRepository;
    private final VideoProcessingProducer videoProcessingProducer;
    private final VideoFileServiceMetrics videoFileServiceMetrics;
    private final VideoFileFinderService videoFileFinderService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VideoFileResponseDTO uploadVideo(MultipartFile file) throws IOException {
        log.info("[uploadVideo] Iniciando upload de vídeo: {}", file.getOriginalFilename());
        videoFileServiceMetrics.incrementUploadRequests();
        Timer.Sample sample = videoFileServiceMetrics.startUploadTimer();

        File tempFile = null;

        try {
            validateFileFormat(file);
            log.debug("[uploadVideo] Formato de arquivo validado");

            videoFileServiceMetrics.setFileSize(file.getSize());

            String originalFileName = file.getOriginalFilename();
            String newFileName = VideoFileNameGenerator.generateUniqueFileName(originalFileName);
            String finalFilePath = VideoFileStorageUtils.buildFilePath(STORAGE_DIR, newFileName);

            log.debug("[uploadVideo] Nome do arquivo gerado: {} | Caminho final: {}", newFileName, finalFilePath);

            VideoFileStorageUtils.createDirectoryIfNotExists(STORAGE_DIR);
            log.debug("[uploadVideo] Diretório de armazenamento verificado/criado: {}", STORAGE_DIR);

            tempFile = File.createTempFile("upload_", "_" + originalFileName);
            file.transferTo(tempFile);
            log.info("[uploadVideo] Arquivo salvo temporariamente em: {}", tempFile.getAbsolutePath());

            VideoValidationExecutor.validateWithFFmpeg(tempFile.getAbsolutePath());
            log.info("[uploadVideo] Validação FFmpeg concluída com sucesso");

            Path targetPath = Path.of(finalFilePath);
            VideoFileStorageUtils.moveFile(tempFile.toPath(), targetPath);
            log.info("[uploadVideo] Arquivo movido para diretório final: {}", finalFilePath);

            VideoFile videoFile = createVideoEntity(file, finalFilePath, newFileName);
            videoFile = videoFileRepository.save(videoFile);
            log.info("[uploadVideo] Entidade VideoFile salva com ID: {}", videoFile.getId());

            videoProcessingProducer.sendVideoId(videoFile.getId());
            log.info("[uploadVideo] Enviado para RabbitMQ: VideoID {}", videoFile.getId());

            videoFileServiceMetrics.incrementUploadSuccess();
            videoFileServiceMetrics.recordUploadDuration(sample);

            log.info("[uploadVideo] Upload concluído com sucesso para o vídeo: {}", videoFile.getVideoFileName());
            return new VideoFileResponseDTO(
                    videoFile.getId(),
                    videoFile.getVideoFileName(),
                    videoFile.getCreatedTimes());

        } catch (Exception e) {
            videoFileServiceMetrics.incrementFileValidationErrors();
            log.error("❌ [uploadVideo] Erro durante upload de vídeo: {}", e.getMessage(), e);
            throw e;
        } finally {
            VideoFileStorageUtils.deleteFileIfExists(tempFile);
            log.debug("[uploadVideo] Arquivo temporário removido: {}",
                    tempFile != null ? tempFile.getAbsolutePath() : "null");
        }
    }

    public VideoFile getVideoById(String id) {
        log.info("[getVideoById] Buscando vídeo por ID: {}", id);
        return videoFileFinderService.findById(id);
    }

    public List<VideoFileListDTO> listAllVideos() {
        log.info("[listAllVideos] Listando todos os vídeos");
        List<VideoFileListDTO> videos = videoFileRepository.findAllVideos();
        if (videos.isEmpty()) {
            log.warn("[listAllVideos] Nenhum vídeo encontrado na base de dados");
            throw new NoVideosFoundException("Nenhum vídeo encontrado.");
        }
        log.info("[listAllVideos] {} vídeos encontrados", videos.size());
        return videos;
    }

    private void validateFileFormat(MultipartFile file) {
        if (file.isEmpty()) {
            log.warn("[validateFileFormat] Arquivo recebido está vazio");
            throw new IllegalArgumentException("O arquivo de vídeo não pode estar vazio.");
        }
        log.debug("[validateFileFormat] Arquivo válido: {}", file.getOriginalFilename());
    }

    private VideoFile createVideoEntity(MultipartFile file, String filePath, String newFileName) throws IOException {
        String extension = newFileName.substring(newFileName.lastIndexOf(".") + 1);
        String duration = VideoDurationUtils.getVideoDurationAsString(filePath);

        log.debug("[createVideoEntity] Criando entidade VideoFile | Nome: {} | Duração: {} | Extensão: {}", newFileName,
                duration, extension);

        VideoFile videoFile = new VideoFile();
        videoFile.setVideoFileName(newFileName);
        videoFile.setVideoFileSize(file.getSize());
        videoFile.setVideoFileFormat(extension);
        videoFile.setVideoDuration(duration);
        videoFile.setVideoFilePath(filePath);
        videoFile.setCreatedTimes(ZonedDateTime.now());
        videoFile.setUpdatedTimes(ZonedDateTime.now());
        videoFile.setStatus(VideoStatusEnum.PROCESSING);

        return videoFile;
    }
}
