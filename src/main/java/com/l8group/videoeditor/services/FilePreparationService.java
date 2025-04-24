package com.l8group.videoeditor.services;

import java.io.File;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.models.VideoFile;

@Service
public class FilePreparationService {

    @Value("${video.upload.dir}")
    private String uploadDir;  // Diretório configurado onde os vídeos estão armazenados localmente

    private static final Logger logger = LoggerFactory.getLogger(FilePreparationService.class);

    /**
     * Retorna o caminho do arquivo de entrada local.
     * Não baixa do S3, apenas confia no caminho local configurado.
     * @param videoFile O objeto que contém os detalhes do arquivo de vídeo.
     * @return O caminho local do arquivo de vídeo.
     */
    public String prepareVideoFileAndInputPath(VideoFile videoFile) {
        // Aqui o nome do arquivo é obtido diretamente do banco de dados
        String fileName = videoFile.getVideoFileName(); // CAMPO CORRETO MAPEADO NO BANCO

        // Concatena o diretório configurado com o nome do arquivo
        String inputFilePath = Paths.get(uploadDir, fileName).toString();


        // Verifica se o arquivo realmente existe no caminho local
        File file = new File(inputFilePath);
        if (file.exists()) {
            logger.info("✅ Arquivo encontrado localmente em: {}", inputFilePath);
            return inputFilePath;  // Retorna o caminho do arquivo encontrado localmente
        } else {
            logger.error("❌ Arquivo NÃO encontrado localmente! Esperado em: {}", inputFilePath);
            throw new RuntimeException("Arquivo de vídeo não encontrado localmente: " + inputFilePath);
        }
    }
}
