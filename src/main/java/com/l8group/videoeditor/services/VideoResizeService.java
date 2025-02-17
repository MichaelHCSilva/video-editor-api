package com.l8group.videoeditor.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.l8group.videoeditor.dtos.VideoResizeResponseDTO;
import com.l8group.videoeditor.enums.VideoStatus;
import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoResize;
import com.l8group.videoeditor.repositories.VideoResizeRepository;
import com.l8group.videoeditor.requests.VideoResizeRequest;
import com.l8group.videoeditor.utils.VideoResolutionsUtils;

@Service
public class VideoResizeService {

    
    private final VideoResizeRepository videoResizeRepository;
    private final VideoFileService videoFileService;

    public VideoResizeService(VideoResizeRepository videoResizeRepository, VideoFileService videoFileService) {
        
        this.videoResizeRepository = videoResizeRepository;
        this.videoFileService = videoFileService;
    }

    
    public VideoResizeResponseDTO resizeVideo(VideoResizeRequest request) throws IOException, InterruptedException {
        // Buscar o vídeo original no banco de dados
        VideoFile originalVideo = videoFileService.getVideoById(UUID.fromString(request.getVideoId()));


        String inputFilePath = originalVideo.getFilePath();
        
        // Criar um arquivo temporário para armazenar o vídeo redimensionado
        String tempFilePath = inputFilePath + "_temp.mp4";

        // Executar o redimensionamento
        resize(inputFilePath, tempFilePath, request.getWidth(), request.getHeight());

        // Substituir o arquivo original pelo redimensionado
        replaceOriginalFile(inputFilePath, tempFilePath);

        // Salvar no banco de dados o redimensionamento
        VideoResize videoResize = saveVideoResize(originalVideo, originalVideo.getFileName(), request.getWidth(), request.getHeight());

        return new VideoResizeResponseDTO(originalVideo.getFileName(), request.getWidth() + "x" + request.getHeight(), videoResize.getCreatedAt());
    }

    public String resize(String inputFilePath, String outputFilePath, int width, int height) throws IOException, InterruptedException {
        if (!VideoResolutionsUtils.isValidResolution(width, height)) {
            throw new VideoProcessingException("Resolução não suportada.");
        }
    
        // Se entrada e saída forem o mesmo arquivo, criar um temporário
        boolean sameFile = inputFilePath.equals(outputFilePath);
        String tempFilePath = sameFile ? inputFilePath + "_temp.mp4" : outputFilePath;
    
        executeResizeCommand(inputFilePath, width, height, tempFilePath);
        verifyResizedFileExistence(tempFilePath);
    
        if (sameFile) {
            replaceOriginalFile(inputFilePath, tempFilePath);
            return inputFilePath;
        } else {
            return tempFilePath;
        }
    }
    

    private void executeResizeCommand(String inputFilePath, int width, int height, String outputFilePath)
            throws IOException, InterruptedException {
        String[] command = {
                "ffmpeg", "-y",
                "-i", inputFilePath,
                "-vf", "scale=" + width + ":" + height,
                "-c:v", "libx264", "-preset", "fast", "-crf", "23",
                "-c:a", "aac", "-strict", "experimental",
                outputFilePath
        };

        Process process = new ProcessBuilder(command).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Erro ao executar o FFmpeg.");
        }
    }

    private void verifyResizedFileExistence(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            throw new IOException("Falha ao criar o arquivo de vídeo redimensionado.");
        }
    }

    private void replaceOriginalFile(String originalFilePath, String tempFilePath) throws IOException {
        File originalFile = new File(originalFilePath);
        File tempFile = new File(tempFilePath);

        if (!tempFile.exists() || tempFile.length() == 0) {
            throw new IOException("Erro ao substituir o arquivo original.");
        }

        Files.move(tempFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private VideoResize saveVideoResize(VideoFile originalVideo, String fileName, int width, int height) {
        VideoResize videoResize = new VideoResize();
        videoResize.setVideoFile(originalVideo);
        videoResize.setFileName(fileName);
        videoResize.setResolution(width + "x" + height);
        videoResize.setCreatedAt(ZonedDateTime.now());
        videoResize.setUploadedAt(ZonedDateTime.now());
        videoResize.setStatus(VideoStatus.PROCESSING);
        return videoResizeRepository.save(videoResize);
    }
}
