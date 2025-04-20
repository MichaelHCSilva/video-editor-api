package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.metrics.VideoResizeServiceMetrics;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoResize;
import com.l8group.videoeditor.rabbit.producer.VideoResizeProducer;
import com.l8group.videoeditor.repositories.VideoResizeRepository;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.utils.FileStorageUtils;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.utils.VideoProcessorUtils;
import com.l8group.videoeditor.utils.VideoResolutionsUtils;
import io.micrometer.core.instrument.Timer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VideoResizeService {

    private static final Logger logger = LoggerFactory.getLogger(VideoResizeService.class);

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    private final VideoFileFinderService videoFileFinderService;
    private final VideoResizeRepository videoResizeRepository;
    private final VideoResizeProducer videoResizeProducer;
    private final VideoResizeServiceMetrics videoResizeServiceMetrics;
    private final VideoStatusManagerService videoStatusManagerService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String resizeVideo(VideoResizeRequest request, String previousFilePath) {
        logger.info(
                "Iniciando redimensionamento do vídeo. videoId={}, width={}, height={}, previousFilePath={}",
                request.getVideoId(), request.getWidth(), request.getHeight(), previousFilePath);
        videoResizeServiceMetrics.incrementResizeRequests();
        videoResizeServiceMetrics.incrementProcessingQueueSize();

        Timer.Sample resizeTimer = videoResizeServiceMetrics.startResizeTimer();
        UUID videoId = UUID.fromString(request.getVideoId());
        VideoFile videoFile = videoFileFinderService.findById(videoId);

        if (!VideoResolutionsUtils.isValidResolution(request.getWidth(), request.getHeight())) {
            logger.error("Resolução não suportada: {}x{}", request.getWidth(), request.getHeight());
            videoResizeServiceMetrics.incrementResizeFailure();
            String supportedResolutions = VideoResolutionsUtils.getSupportedResolutionsAsString();
            throw new IllegalArgumentException(
                    String.format("Resolução não suportada: %dx%d. As resoluções suportadas são: %s",
                                  request.getWidth(), request.getHeight(), supportedResolutions));
        }

        String inputFilePath = FileStorageUtils.resolveInputFilePath(
                previousFilePath, videoFile.getVideoFilePath());

        FileStorageUtils.validateInputFileExists(
                inputFilePath, () -> videoResizeServiceMetrics.incrementResizeFailure());

        FileStorageUtils.createDirectoryIfNotExists(TEMP_DIR);
        Path outputPath = Paths.get(
                TEMP_DIR, VideoFileNameGenerator.generateFileNameWithSuffix(videoFile.getVideoFileName(), "resize"));
        String outputFilePath = outputPath.toString();
        logger.info("Arquivo de saída definido como: {}", outputFilePath);

        boolean resizeSuccess = VideoProcessorUtils.resizeVideo(
                inputFilePath, outputFilePath, request.getWidth(), request.getHeight());

        if (!resizeSuccess) {
            logger.error("Falha ao processar o redimensionamento do vídeo.");
            videoResizeServiceMetrics.incrementResizeFailure();
            throw new RuntimeException("Falha ao processar o redimensionamento do vídeo.");
        }

        try {
            long fileSize = Files.size(outputPath);
            videoResizeServiceMetrics.setResizeFileSize(fileSize);
            logger.info("Tamanho do arquivo redimensionado: {} bytes", fileSize);
        } catch (Exception e) {
            logger.error("Erro ao obter tamanho do arquivo redimensionado: {}", e.getMessage());
        }

        VideoResize videoResize = createVideoResizeEntity(videoFile, request);
        VideoResize savedVideoResize = videoResizeRepository.save(videoResize);
        logger.info("Registro de redimensionamento salvo no banco de dados. ID={}", savedVideoResize.getId());

        videoResizeProducer.sendMessage(savedVideoResize.getId().toString());

        videoResizeServiceMetrics.recordResizeDuration(resizeTimer);
        videoResizeServiceMetrics.decrementProcessingQueueSize();
        videoResizeServiceMetrics.incrementResizeSuccess();

        videoStatusManagerService.updateEntityStatus(
                videoResizeRepository, savedVideoResize.getId(), VideoStatusEnum.COMPLETED,
                "VideoResizeService - Conclusão");

        return outputFilePath;
    }

    private VideoResize createVideoResizeEntity(VideoFile videoFile, VideoResizeRequest request) {
        VideoResize videoResize = new VideoResize();
        videoResize.setVideoFile(videoFile);
        videoResize.setTargetResolution(request.getWidth() + "x" + request.getHeight());
        videoResize.setCreatedTimes(ZonedDateTime.now());
        videoResize.setUpdatedTimes(ZonedDateTime.now());
        videoResize.setStatus(VideoStatusEnum.PROCESSING);
        return videoResize;
    }

    public void deleteTemporaryFiles(String filePath) {
        FileStorageUtils.deleteFileIfExists(Paths.get(filePath).toFile());
    }
}