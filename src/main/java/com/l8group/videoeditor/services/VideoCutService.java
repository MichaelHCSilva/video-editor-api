package com.l8group.videoeditor.services;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.models.VideoCut;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoCutRepository;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.utils.VideoDurationUtils;
import com.l8group.videoeditor.utils.VideoProcessorUtils;
import com.l8group.videoeditor.utils.VideoUtils;

@Service
public class VideoCutService {

    private static final Logger logger = LoggerFactory.getLogger(VideoCutService.class);

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    private final VideoFileRepository videoFileRepository;
    private final VideoCutRepository videoCutRepository;

    public VideoCutService(VideoFileRepository videoFileRepository, VideoCutRepository videoCutRepository) {
        this.videoFileRepository = videoFileRepository;
        this.videoCutRepository = videoCutRepository;
    }

    @Transactional
    public String cutVideo(VideoCutRequest request, String previousFilePath) {
        logger.info("Iniciando corte do vídeo. videoId={}, startTime={}, endTime={}, previousFilePath={}",
                request.getVideoId(), request.getStartTime(), request.getEndTime(), previousFilePath);
        UUID videoId = UUID.fromString(request.getVideoId());

        // Verifica se o vídeo existe no banco
        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado para o ID: " + videoId));

        logger.info("Vídeo encontrado: {} (caminho: {})", videoFile.getFileName(), videoFile.getFilePath());

        // Determina qual arquivo será usado como entrada
        String inputFilePath = (previousFilePath != null && !previousFilePath.isEmpty())
                ? previousFilePath
                : videoFile.getFilePath();

        logger.info("Arquivo de entrada definido como: {}", inputFilePath);

        // Verifica se o arquivo de entrada realmente existe
        File inputFile = new File(inputFilePath);
        if (!inputFile.exists()) {
            logger.error("Arquivo de entrada não encontrado: {}", inputFilePath);
            throw new RuntimeException("Arquivo de entrada não encontrado: " + inputFilePath);
        }

        String originalFileName = videoFile.getFileName();
        String shortUUID = VideoUtils.generateShortUUID();
        String formattedDate = VideoUtils.formatDate(LocalDate.now());

        // Nome do arquivo cortado
        String cutFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                + "_" + shortUUID + formattedDate + "_cut." + videoFile.getFileFormat();

        String outputFilePath = Paths.get(TEMP_DIR, cutFileName).toString();
        logger.info("Arquivo de saída definido como: {}", outputFilePath);

        // Garante que o diretório temporário existe
        createTempDirectory();

        // Convertendo os tempos de corte para segundos
        int startTime = VideoDurationUtils.convertTimeToSeconds(request.getStartTime());
        int endTime = VideoDurationUtils.convertTimeToSeconds(request.getEndTime());

        logger.info("Tempo de corte convertido: start={}s, end={}s", startTime, endTime);

        // Realiza o corte do vídeo
        logger.info("Iniciando corte do vídeo: {} → {}", inputFilePath, outputFilePath);
        boolean success = VideoProcessorUtils.cutVideo(inputFilePath, outputFilePath, request.getStartTime(),
                request.getEndTime());

        if (!success) {
            logger.error("Falha ao processar o corte do vídeo.");
            throw new RuntimeException("Falha ao processar o corte do vídeo.");
        }
        logger.info("Corte concluído com sucesso.");

        // Calcula a duração do corte
        int cutDuration = endTime - startTime;
        String durationFormatted = VideoDurationUtils.formatSecondsToTime(cutDuration);
        logger.info("Duração do vídeo cortado: {}", durationFormatted);

        // Salva os dados do corte no banco de dados
        VideoCut videoCut = new VideoCut();
        videoCut.setVideoFile(videoFile);
        videoCut.setDuration(durationFormatted);
        videoCut.setCreatedAt(ZonedDateTime.now());
        videoCut.setUpdatedAt(ZonedDateTime.now());
        videoCut.setStatus(VideoStatus.PROCESSING);

        videoCut = videoCutRepository.save(videoCut);
        logger.info("Registro de corte salvo no banco de dados. ID={}", videoCut.getId());

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
