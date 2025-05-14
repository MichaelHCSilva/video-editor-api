package com.l8group.videoeditor.services;

import java.io.File;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.models.VideoFile;

@Service
public class VideoFilePreparationService {

    @Value("${video.upload.dir}")
    private String uploadDir; 

    private static final Logger logger = LoggerFactory.getLogger(VideoFilePreparationService.class);

    /**
     * @param videoFile 
     * @return 
     */
    public String prepareVideoFileAndInputPath(VideoFile videoFile) {
        String fileName = videoFile.getVideoFileName(); 

        String inputFilePath = Paths.get(uploadDir, fileName).toString();

        File file = new File(inputFilePath);
        if (file.exists()) {
            logger.info("Arquivo encontrado localmente em: {}", inputFilePath);
            return inputFilePath;
        } else {
            logger.error("Arquivo NÃO encontrado localmente! Esperado em: {}", inputFilePath);
            throw new RuntimeException("Arquivo de vídeo não encontrado localmente: " + inputFilePath);
        }
    }
}
