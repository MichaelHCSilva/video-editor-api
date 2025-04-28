package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.InvalidCutTimeException;
import com.l8group.videoeditor.exceptions.InvalidMediaPropertiesException;
import com.l8group.videoeditor.exceptions.VideoMetadataException;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.metrics.VideoCutServiceMetrics;
import com.l8group.videoeditor.models.VideoCut;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.rabbit.producer.VideoCutProducer;
import com.l8group.videoeditor.repositories.VideoCutRepository;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.utils.VideoFileStorageUtils;
import com.l8group.videoeditor.utils.VideoDurationUtils;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.utils.VideoProcessorUtils;
import com.l8group.videoeditor.validation.VideoAudioValidator;
import com.l8group.videoeditor.validation.VideoCutValidator;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCutService {

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    @Value("${video.upload.dir}")
    private String uploadDir;

    private final VideoCutRepository videoCutRepository;
    private final VideoCutProducer videoCutProducer;
    private final VideoCutServiceMetrics videoCutServiceMetrics;
    private final VideoStatusManagerService videoStatusManagerService;
    private final VideoFileFinderService videoFileFinderService;
    private final Validator validator;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String cutVideo(VideoCutRequest request, String previousFilePath) {
        log.info("Iniciando corte do vídeo. videoId={}, startTime={}, endTime={}, previousFilePath={}",
                request.getVideoId(), request.getStartTime(), request.getEndTime(), previousFilePath);

        videoCutServiceMetrics.incrementCutRequests();
        VideoFile videoFile = videoFileFinderService.findById(UUID.fromString(request.getVideoId()));
        String inputFilePath = VideoFileStorageUtils.buildFilePath(uploadDir, videoFile.getVideoFileName());
        if (!new File(inputFilePath).exists()) throw new VideoProcessingException("Arquivo de vídeo não encontrado.");

        // Validar o VideoCutRequest usando as anotações
        Set<ConstraintViolation<VideoCutRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Erros na requisição de corte: ");
            for (ConstraintViolation<VideoCutRequest> violation : violations) {
                errorMessage.append(violation.getMessage()).append("; ");
            }
            throw new IllegalArgumentException(errorMessage.toString());
        }

        String videoDuration;
        try {
            videoDuration = VideoDurationUtils.getVideoDurationAsString(inputFilePath);
            log.info("Duração total do vídeo: {}", videoDuration);
        } catch (IOException e) {
            log.error("Erro ao obter duração do vídeo: {}", e.getMessage(), e);
            throw new VideoMetadataException("Erro ao obter duração do vídeo.");
        }

        int startTime = VideoDurationUtils.convertTimeToSeconds(request.getStartTime());
        int endTime = VideoDurationUtils.convertTimeToSeconds(request.getEndTime());
        try {
            VideoCutValidator.validateCutTimes(startTime, endTime, videoFile);
        } catch (InvalidCutTimeException e) {
            throw e; 
        }

        try {
            VideoAudioValidator.validateAudioProperties(inputFilePath, startTime, endTime);
        } catch (InvalidMediaPropertiesException e) {
            log.error("Problema nas propriedades de áudio do vídeo: {}", e.getMessage());
            throw e;
        }

        VideoFileStorageUtils.createDirectoryIfNotExists(TEMP_DIR);
        String outputFilePath = Paths.get(TEMP_DIR, VideoFileNameGenerator.generateFileNameWithSuffix(
                videoFile.getVideoFileName(), "cut")).toString();

        videoCutServiceMetrics.incrementProcessingQueueSize();
        Timer.Sample timer = videoCutServiceMetrics.startCutTimer();
        boolean success = VideoProcessorUtils.cutVideo(inputFilePath, outputFilePath, request.getStartTime(), request.getEndTime());
        videoCutServiceMetrics.recordCutDuration(timer);
        videoCutServiceMetrics.decrementProcessingQueueSize();

        if (!success) {
            videoCutServiceMetrics.incrementCutFailure();
            throw new VideoProcessingException("Falha ao processar o corte do vídeo.");
        }

        int cutDurationSeconds = endTime - startTime;
        VideoCut videoCut = new VideoCut();
        videoCut.setVideoFile(videoFile);
        videoCut.setVideoCutDuration(VideoDurationUtils.formatSecondsToTime(cutDurationSeconds));
        videoCut.setCreatedTimes(ZonedDateTime.now());
        videoCut.setUpdatedTimes(ZonedDateTime.now());
        videoCut.setStatus(VideoStatusEnum.PROCESSING);
        videoCut = videoCutRepository.save(videoCut);

        videoCutProducer.sendVideoCutId(videoCut.getId());
        videoStatusManagerService.updateEntityStatus(videoCutRepository, videoCut.getId(), VideoStatusEnum.COMPLETED, "CutService - Conclusão");

        return outputFilePath;
    }

    public void deleteTemporaryFiles(String filePath) {
        VideoFileStorageUtils.deleteFileIfExists(new File(filePath));
    }
}