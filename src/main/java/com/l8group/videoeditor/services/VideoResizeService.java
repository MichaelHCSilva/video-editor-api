package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.exceptions.InvalidResizeParameterException;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.metrics.VideoResizeMetrics;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoResize;
import com.l8group.videoeditor.rabbit.producer.VideoResizeProducer;
import com.l8group.videoeditor.repositories.VideoResizeRepository;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.utils.VideoFileNameGenerator;
import com.l8group.videoeditor.utils.VideoFileStorageUtils;
import com.l8group.videoeditor.utils.VideoProcessorUtils;
import com.l8group.videoeditor.validation.VideoResizeValidation;

//import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoResizeService {

    private final VideoResizeRepository videoResizeRepository;
    private final VideoResizeProducer videoResizeProducer;
    private final VideoResizeMetrics videoResizeMetrics;
    private final VideoFileFinderService videoFileFinderService;
    private final Validator validator;
    private final VideoStatusService videoStatusManagerService;

    @Value("${video.temp.dir}")
    private String tempDir;

    @Transactional
    public String resizeVideo(VideoResizeRequest request, String previousFilePath) {
        log.info("[resizeVideo] Iniciando redimensionamento | VideoId: {} | Dimensões: {}x{}",
                request.getVideoId(), request.getWidth(), request.getHeight());

        videoResizeMetrics.incrementResizeRequests();

        log.debug("[resizeVideo] Validando request via Bean Validation...");
        Set<ConstraintViolation<VideoResizeRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(" | "));
            log.error("[resizeVideo] Erros de validação: {}", errorMessages);
            throw new InvalidResizeParameterException("Parâmetros inválidos: " + errorMessages);
        }

        log.debug("[resizeVideo] Validando dimensões permitidas...");
        VideoResizeValidation.validate(request.getWidth(), request.getHeight());

        log.debug("[resizeVideo] Buscando vídeo no banco de dados...");
        VideoFile videoFile = videoFileFinderService.findById(request.getVideoId());

        String inputFilePath = previousFilePath != null ? previousFilePath : videoFile.getVideoFilePath();
        log.debug("[resizeVideo] Caminho do vídeo de entrada: {}", inputFilePath);

        validateInputFileExists(inputFilePath);

        log.debug("[resizeVideo] Preparando caminho para o arquivo de saída...");
        String outputFilePath = prepareOutputFile(videoFile.getVideoFileName());

        VideoResize resizeEntity = saveResizeEntity(videoFile, request);

        log.info("[resizeVideo] Processando redimensionamento...");
        Timer.Sample timerSample = videoResizeMetrics.startResizeTimer(); 


        try {
            processResize(inputFilePath, outputFilePath, request);

            videoResizeMetrics.recordResizeDuration(timerSample); 
            videoResizeMetrics.decrementProcessingQueueSize(); 

            videoStatusManagerService.updateEntityStatus(
                    videoResizeRepository, resizeEntity.getId(), VideoStatusEnum.COMPLETED,
                    "VideoResizeService - Conclusão");
            videoResizeMetrics.incrementResizeSuccess();

        } catch (VideoProcessingException e) {
            videoResizeMetrics.recordResizeDuration(timerSample); 
            videoResizeMetrics.decrementProcessingQueueSize(); 
            videoResizeMetrics.incrementResizeFailure();
            videoStatusManagerService.updateEntityStatus(
                    videoResizeRepository, resizeEntity.getId(), VideoStatusEnum.ERROR,
                    "VideoResizeService - Processamento Falhou");
            throw e;
        } catch (Exception e) {
            videoResizeMetrics.recordResizeDuration(timerSample); 
            videoResizeMetrics.decrementProcessingQueueSize(); 
            videoResizeMetrics.incrementResizeFailure();
            videoStatusManagerService.updateEntityStatus(
                    videoResizeRepository, resizeEntity.getId(), VideoStatusEnum.ERROR,
                    "VideoResizeService - Erro Inesperado");
            throw new VideoProcessingException("Erro inesperado ao redimensionar vídeo.", e);
        }

        log.debug("[resizeVideo] Enviando mensagem para fila RabbitMQ...");
        videoResizeProducer.sendMessage(videoFile.getId().toString());

        log.info("[resizeVideo] Redimensionamento finalizado com sucesso | Output: {}", outputFilePath);
        return outputFilePath;
    }

    private void validateInputFileExists(String filePath) {
        log.debug("[validateInputFileExists] Verificando existência do arquivo de entrada...");
        File file = new File(filePath);
        if (!file.exists()) {
            log.error("[validateInputFileExists] Arquivo de entrada não encontrado: {}", filePath);
            throw new VideoProcessingException("Vídeo inexistente ou removido para o ID especificado.");
        }
        log.debug("[validateInputFileExists] Arquivo de entrada encontrado.");
    }

    private String prepareOutputFile(String originalFileName) {
        log.debug("[prepareOutputFile] Criando diretório temporário se necessário...");
        VideoFileStorageUtils.createDirectoryIfNotExists(tempDir);

        String outputPath = VideoFileStorageUtils.buildFilePath(
                tempDir, VideoFileNameGenerator.generateFileNameWithSuffix(originalFileName, "resize"));

        log.debug("[prepareOutputFile] Caminho do arquivo de saída preparado: {}", outputPath);
        return outputPath;
    }

    private void processResize(String inputFilePath, String outputFilePath, VideoResizeRequest request) {
        try {
            log.debug("[processResize] Chamando utilitário de redimensionamento...");
            boolean success = VideoProcessorUtils.resizeVideo(
                    inputFilePath, outputFilePath, request.getWidth(), request.getHeight());

            if (!success) {
                log.error("[processResize] Redimensionamento falhou.");
                throw new VideoProcessingException("Erro ao redimensionar vídeo.");
            }

            try {
                long fileSize = Files.size(Paths.get(outputFilePath));
                videoResizeMetrics.setResizeFileSize(fileSize);
                log.info("[processResize] Tamanho do vídeo redimensionado: {} bytes", fileSize);
            } catch (Exception e) {
                log.warn("[processResize] Erro ao obter o tamanho do arquivo de saída: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("[processResize] Falha inesperada no redimensionamento do vídeo.", e);
            throw new VideoProcessingException("Erro inesperado ao redimensionar vídeo.", e);
        }
    }

    private VideoResize saveResizeEntity(VideoFile videoFile, VideoResizeRequest request) {
        log.debug("[saveResizeEntity] Persistindo entidade de redimensionamento...");
        VideoResize resize = new VideoResize();
        resize.setVideoFile(videoFile);
        resize.setTargetResolution(request.getWidth() + "x" + request.getHeight());
        resize.setStatus(VideoStatusEnum.PROCESSING);
        resize.setCreatedTimes(ZonedDateTime.now());
        resize.setUpdatedTimes(ZonedDateTime.now());
        return videoResizeRepository.save(resize);
    }

    public void deleteTemporaryFiles(String filePath) {
        log.info("[deleteTemporaryFiles] Excluindo arquivo temporário: {}", filePath);
        VideoFileStorageUtils.deleteFileIfExists(new File(filePath));
    }
}
