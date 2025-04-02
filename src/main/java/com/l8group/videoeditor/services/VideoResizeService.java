package com.l8group.videoeditor.services;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.metrics.VideoResizeServiceMetrics;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoResize;
import com.l8group.videoeditor.rabbit.producer.VideoResizeProducer;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.repositories.VideoResizeRepository;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.utils.VideoProcessorUtils;
import com.l8group.videoeditor.utils.VideoResolutionsUtils;
import com.l8group.videoeditor.utils.VideoUtils;

import io.micrometer.core.instrument.Timer;

@Service
public class VideoResizeService {

    private static final Logger logger = LoggerFactory.getLogger(VideoResizeService.class);

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    private final VideoFileRepository videoFileRepository;
    private final VideoResizeRepository videoResizeRepository;
    private final VideoResizeProducer videoResizeProducer;
    private final VideoResizeServiceMetrics videoResizeServiceMetrics;
    private final VideoStatusManagerService videoStatusManagerService; // Adicionado

    public VideoResizeService(VideoFileRepository videoFileRepository, VideoResizeRepository videoResizeRepository,
            VideoResizeProducer videoResizeProducer, VideoResizeServiceMetrics videoResizeServiceMetrics,
            VideoStatusManagerService videoStatusManagerService) {
        this.videoFileRepository = videoFileRepository;
        this.videoResizeRepository = videoResizeRepository;
        this.videoResizeProducer = videoResizeProducer;
        this.videoResizeServiceMetrics = videoResizeServiceMetrics;
        this.videoStatusManagerService = videoStatusManagerService; // Adicionado

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String resizeVideo(VideoResizeRequest request, String previousFilePath) {
        logger.info("Iniciando redimensionamento do vídeo. videoId={}, width={}, height={}, previousFilePath={}",
                request.getVideoId(), request.getWidth(), request.getHeight(), previousFilePath);

        videoResizeServiceMetrics.incrementResizeRequests();
        videoResizeServiceMetrics.incrementProcessingQueueSize();

        Timer.Sample sample = videoResizeServiceMetrics.startResizeTimer();

        UUID videoId = UUID.fromString(request.getVideoId());

        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado para o ID: " + videoId));

        logger.info("Vídeo encontrado: {} (caminho: {})", videoFile.getVideoFileName(), videoFile.getVideoFilePath());

        // Valida a resolução do vídeo
        if (!VideoResolutionsUtils.isValidResolution(request.getWidth(), request.getHeight())) {
            logger.error("Resolução não suportada: {}x{}", request.getWidth(), request.getHeight());
            videoResizeServiceMetrics.incrementResizeFailure();
            throw new IllegalArgumentException(
                    "Resolução não suportada: " + request.getWidth() + "x" + request.getHeight());
        }

        // Determina qual arquivo usar como entrada
        String inputFilePath = (previousFilePath != null && !previousFilePath.isEmpty())
                ? previousFilePath
                : videoFile.getVideoFilePath();

        logger.info("Arquivo de entrada definido como: {}", inputFilePath);

        // Verifica se o arquivo de entrada realmente existe
        File inputFile = new File(inputFilePath);
        if (!inputFile.exists()) {
            logger.error("Arquivo de entrada não encontrado: {}", inputFilePath);
            videoResizeServiceMetrics.incrementResizeFailure();
            throw new RuntimeException("Arquivo de entrada não encontrado: " + inputFilePath);
        }

        String originalFileName = videoFile.getVideoFileName();
        String shortUUID = VideoUtils.generateShortUuid();
        String formattedDate = VideoUtils.formatDateToCompactString(LocalDate.now());

        // Nome do arquivo redimensionado
        String resizeFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                + "_" + shortUUID + formattedDate + "_resize." + videoFile.getVideoFileFormat();

        // Caminho do arquivo temporário de saída
        String outputFilePath = Paths.get(TEMP_DIR, resizeFileName).toString();
        logger.info("Arquivo de saída definido como: {}", outputFilePath);

        // Garante que o diretório temporário existe
        createTempDirectory();

        // Realiza o redimensionamento do vídeo
        logger.info("Iniciando redimensionamento do vídeo: {} → {}", inputFilePath, outputFilePath);
        boolean success = VideoProcessorUtils.resizeVideo(inputFilePath, outputFilePath, request.getWidth(),
                request.getHeight());

        if (!success) {
            logger.error("Falha ao processar o redimensionamento do vídeo.");
            videoResizeServiceMetrics.incrementResizeFailure();
            throw new RuntimeException("Falha ao processar o redimensionamento do vídeo.");
        }
        logger.info("Redimensionamento concluído com sucesso.");
        videoResizeServiceMetrics.incrementResizeSuccess();

        try {
            long fileSize = Files.size(Paths.get(outputFilePath));
            videoResizeServiceMetrics.setResizeFileSize(fileSize);
            logger.info("Tamanho do arquivo redimensionado: {} bytes", fileSize);
        } catch (Exception e) {
            logger.error("Erro ao obter tamanho do arquivo redimensionado: {}", e.getMessage());
        }

        // Registra a duração do redimensionamento
        videoResizeServiceMetrics.recordResizeDuration(sample);

        // Salva os dados no banco de dados
        VideoResize videoResize = new VideoResize();
        videoResize.setVideoFile(videoFile);
        videoResize.setTargetResolution(request.getWidth() + "x" + request.getHeight());
        videoResize.setCreatedTimes(ZonedDateTime.now());
        videoResize.setUpdatedTimes(ZonedDateTime.now());
        videoResize.setStatus(VideoStatusEnum.PROCESSING);

        videoResize = videoResizeRepository.save(videoResize);
        logger.info("Registro de redimensionamento salvo no banco de dados. ID={}", videoResize.getId());

        videoResizeProducer.sendMessage(videoResize.getId().toString());

        videoResizeServiceMetrics.decrementProcessingQueueSize();

        // Atualizar o status do VideoResize após o envio para o RabbitMQ e outras operações
        videoStatusManagerService.updateEntityStatus(videoResizeRepository, videoResize.getId(),
                VideoStatusEnum.COMPLETED, "VideoResizeService - Conclusão");

        return outputFilePath;
    }

    private void createTempDirectory() {
        File tempDir = new File(TEMP_DIR);
        if (!tempDir.exists()) {
            boolean created = tempDir.mkdirs();
            if (created) {
                logger.info("Diretório temporário criado: {}", TEMP_DIR);
            } else {
                logger.error("Erro ao criar diretório temporário: {}", TEMP_DIR);
                throw new RuntimeException("Erro ao criar diretório temporário.");
            }
        }
    }

    // Método para remover arquivos temporários após a consolidação final
    public void deleteTemporaryFiles(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                logger.info("Arquivo temporário removido com sucesso: {}", filePath);
            } else {
                logger.error("Falha ao excluir arquivo temporário: {}", filePath);
            }
        } else {
            logger.warn("Tentativa de remover arquivo inexistente: {}", filePath);
        }
    }
}
