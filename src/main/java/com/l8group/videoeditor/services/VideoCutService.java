package com.l8group.videoeditor.services;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.metrics.VideoCutServiceMetrics;
import com.l8group.videoeditor.models.VideoCut;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.rabbit.producer.VideoCutProducer;
import com.l8group.videoeditor.repositories.VideoCutRepository;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.utils.VideoDurationUtils;
import com.l8group.videoeditor.utils.VideoProcessorUtils;
import com.l8group.videoeditor.utils.VideoUtils;

import io.micrometer.core.instrument.Timer;

@Service
public class VideoCutService {

    private static final Logger logger = LoggerFactory.getLogger(VideoCutService.class);

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    private final VideoFileRepository videoFileRepository;
    private final VideoCutRepository videoCutRepository;
    private final VideoCutProducer videoCutProducer;
    private final VideoCutServiceMetrics videoCutServiceMetrics;
    private final VideoStatusManagerService videoStatusManagerService;

    public VideoCutService(VideoFileRepository videoFileRepository, VideoCutRepository videoCutRepository,
            VideoCutProducer videoCutProducer, VideoCutServiceMetrics videoCutServiceMetrics,
            VideoStatusManagerService videoStatusManagerService) {
        this.videoFileRepository = videoFileRepository;
        this.videoCutRepository = videoCutRepository;
        this.videoCutProducer = videoCutProducer;
        this.videoCutServiceMetrics = videoCutServiceMetrics;
        this.videoStatusManagerService = videoStatusManagerService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String cutVideo(VideoCutRequest request, String previousFilePath) {
        logger.info("Iniciando corte do vídeo. videoId={}, startTime={}, endTime={}, previousFilePath={}",
                request.getVideoId(), request.getStartTime(), request.getEndTime(), previousFilePath);

        videoCutServiceMetrics.incrementCutRequests();

        UUID videoId = UUID.fromString(request.getVideoId());

        // Verifica se o vídeo existe no banco
        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado para o ID: " + videoId));

        logger.info("Vídeo encontrado: {} (caminho: {})", videoFile.getVideoFileName(), videoFile.getVideoFilePath());

        // Determina qual arquivo será usado como entrada
        String inputFilePath = (previousFilePath != null && !previousFilePath.isEmpty())
                ? previousFilePath
                : videoFile.getVideoFilePath();

        logger.info("Arquivo de entrada definido como: {}", inputFilePath);

        // Verifica se o arquivo de entrada realmente existe
        File inputFile = new File(inputFilePath);
        if (!inputFile.exists()) {
            logger.error("Arquivo de entrada não encontrado: {}", inputFilePath);
            videoCutServiceMetrics.incrementCutFailure();
            throw new RuntimeException("Arquivo de entrada não encontrado: " + inputFilePath);
        }

        String originalFileName = videoFile.getVideoFileName();
        String shortUUID = VideoUtils.generateShortUuid();
        String formattedDate = VideoUtils.formatDateToCompactString(LocalDate.now());

        // Nome do arquivo cortado
        String cutFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                + "_" + shortUUID + formattedDate + "_cut." + videoFile.getVideoFileFormat();

        String outputFilePath = Paths.get(TEMP_DIR, cutFileName).toString();
        logger.info("Arquivo de saída definido como: {}", outputFilePath);

        // Garante que o diretório temporário existe
        createTempDirectory();

        // Convertendo os tempos de corte para segundos
        int startTime = VideoDurationUtils.convertTimeToSeconds(request.getStartTime());
        int endTime = VideoDurationUtils.convertTimeToSeconds(request.getEndTime());

        logger.info("Tempo de corte convertido: start={}s, end={}s", startTime, endTime);

        videoCutServiceMetrics.incrementProcessingQueueSize(); // 🔹 Adiciona à fila
        Timer.Sample timerSample = videoCutServiceMetrics.startCutTimer(); // 🔹 Inicia o timer

        // Realiza o corte do vídeo
        logger.info("Iniciando corte do vídeo: {} → {}", inputFilePath, outputFilePath);
        boolean success = VideoProcessorUtils.cutVideo(inputFilePath, outputFilePath, request.getStartTime(),
                request.getEndTime());

        if (!success) {
            logger.error("Falha ao processar o corte do vídeo.");
            videoCutServiceMetrics.incrementCutFailure(); // 🔹 Incrementa falhas
            videoCutServiceMetrics.decrementProcessingQueueSize(); // 🔹 Remove da fila
            throw new RuntimeException("Falha ao processar o corte do vídeo.");
        }

        videoCutServiceMetrics.incrementCutSuccess(); // 🔹 Incrementa sucesso
        videoCutServiceMetrics.recordCutDuration(timerSample); // 🔹 Registra duração do corte

        File outputFile = new File(outputFilePath);
        if (outputFile.exists()) {
            videoCutServiceMetrics.setCutFileSize(outputFile.length()); // 🔹 Atualiza métrica de tamanho
        }

        logger.info("Corte concluído com sucesso.");
        videoCutServiceMetrics.decrementProcessingQueueSize(); // 🔹 Remove da fila

        // Calcula a duração do corte
        int cutDuration = endTime - startTime;
        String durationFormatted = VideoDurationUtils.formatSecondsToTime(cutDuration);
        logger.info("Duração do vídeo cortado: {}", durationFormatted);

        // Salva os dados do corte no banco de dados
        VideoCut videoCut = new VideoCut();
        videoCut.setVideoFile(videoFile);
        videoCut.setVideoCutDuration(durationFormatted);
        videoCut.setCreatedTimes(ZonedDateTime.now());
        videoCut.setUpdatedTimes(ZonedDateTime.now());
        videoCut.setStatus(VideoStatusEnum.PROCESSING);

        videoCut = videoCutRepository.save(videoCut);
        logger.info("Registro de corte salvo no banco de dados. ID={}, Tempo de registro: {}", videoCut.getId(),
                LocalDateTime.now());

        videoCutProducer.sendVideoCutId(videoCut.getId());

        // Atualizar o status do VideoCut após o envio para o RabbitMQ e outras
        // operações
        videoStatusManagerService.updateEntityStatus(videoCutRepository, videoCut.getId(), VideoStatusEnum.COMPLETED,
                "CutService - Conclusão");

        return outputFilePath;
    }

    // Garante que o diretório temporário existe
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

    // Remove arquivos temporários após a consolidação final
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
