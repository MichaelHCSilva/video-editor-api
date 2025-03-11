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
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoResize;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.repositories.VideoResizeRepository;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.utils.VideoProcessorUtils;
import com.l8group.videoeditor.utils.VideoResolutionsUtils;
import com.l8group.videoeditor.utils.VideoUtils;

@Service
public class VideoResizeService {

    private static final Logger logger = LoggerFactory.getLogger(VideoResizeService.class);

    @Value("${video.temp.dir}")
    private String TEMP_DIR;

    private final VideoFileRepository videoFileRepository;
    private final VideoResizeRepository videoResizeRepository;

    public VideoResizeService(VideoFileRepository videoFileRepository, VideoResizeRepository videoResizeRepository) {
        this.videoFileRepository = videoFileRepository;
        this.videoResizeRepository = videoResizeRepository;
    }

    @Transactional
    public String resizeVideo(VideoResizeRequest request, String previousFilePath) {
        logger.info("Iniciando redimensionamento do vídeo. videoId={}, width={}, height={}, previousFilePath={}",
                request.getVideoId(), request.getWidth(), request.getHeight(), previousFilePath);

        UUID videoId = UUID.fromString(request.getVideoId());

        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado para o ID: " + videoId));

        logger.info("Vídeo encontrado: {} (caminho: {})", videoFile.getFileName(), videoFile.getFilePath());

        // Valida a resolução do vídeo
        if (!VideoResolutionsUtils.isValidResolution(request.getWidth(), request.getHeight())) {
            logger.error("Resolução não suportada: {}x{}", request.getWidth(), request.getHeight());
            throw new IllegalArgumentException(
                    "Resolução não suportada: " + request.getWidth() + "x" + request.getHeight());
        }

        // Determina qual arquivo usar como entrada
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

        // Nome do arquivo redimensionado
        String resizeFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                + "_" + shortUUID + formattedDate + "_resize." + videoFile.getFileFormat();

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
            throw new RuntimeException("Falha ao processar o redimensionamento do vídeo.");
        }
        logger.info("Redimensionamento concluído com sucesso.");

        // Salva os dados no banco de dados
        VideoResize videoResize = new VideoResize();
        videoResize.setVideoFile(videoFile);
        videoResize.setResolution(request.getWidth() + "x" + request.getHeight());
        videoResize.setCreatedAt(ZonedDateTime.now());
        videoResize.setUpdatedAt(ZonedDateTime.now());
        videoResize.setStatus(VideoStatus.PROCESSING);

        videoResize = videoResizeRepository.save(videoResize);
        logger.info("Registro de redimensionamento salvo no banco de dados. ID={}", videoResize.getId());

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
