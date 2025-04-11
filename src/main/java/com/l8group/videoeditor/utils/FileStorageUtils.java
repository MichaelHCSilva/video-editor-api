package com.l8group.videoeditor.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileStorageUtils {

    private static final Logger log = LoggerFactory.getLogger(FileStorageUtils.class);

    /**
     * Constrói um caminho de arquivo com base no diretório base e no nome do arquivo.
     */
    public static String buildFilePath(String baseDir, String fileName) {
        return baseDir + File.separator + fileName;
    }

    /**
     * Cria um diretório se ele ainda não existir.
     */
    public static void createDirectoryIfNotExists(String dir) {
        File directory = new File(dir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("Diretório criado: {}", dir);
            }
        }
    }

    /**
     * Move um arquivo de um local para outro.
     */
    public static void moveFile(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Arquivo movido de {} para {}", source, target);
    }

    /**
     * Deleta um arquivo se ele existir, com log de sucesso ou falha.
     */
    public static void deleteFileIfExists(File file) {
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("Arquivo temporário removido: {}", file.getAbsolutePath());
            } else {
                log.warn("Falha ao remover arquivo temporário: {}", file.getAbsolutePath());
            }
        }
    }
}
