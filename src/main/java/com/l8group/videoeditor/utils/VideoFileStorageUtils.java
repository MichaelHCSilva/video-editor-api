package com.l8group.videoeditor.utils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoFileStorageUtils {

    private static final Logger log = LoggerFactory.getLogger(VideoFileStorageUtils.class);

    public static String resolveInputFilePath(String previousFilePath, String defaultPath) {
        String path = (previousFilePath != null && !previousFilePath.isEmpty())
                ? previousFilePath
                : defaultPath;
        log.info("Arquivo de entrada definido como: {}", path);
        return path;
    }

    /**
     * @param inputFilePath
     * @param onFailure
     */
    public static void validateInputFileExists(String inputFilePath, Runnable onFailure) {
        if (isRemotePath(inputFilePath)) {
            if (!urlExists(inputFilePath)) {
                log.error("Arquivo remoto não encontrado: {}", inputFilePath);
                if (onFailure != null)
                    onFailure.run();
                throw new RuntimeException("Arquivo remoto não encontrado: " + inputFilePath);
            }
        } else {
            File inputFile = new File(inputFilePath);
            if (!inputFile.exists()) {
                log.error("Arquivo local não encontrado: {}", inputFilePath);
                if (onFailure != null)
                    onFailure.run();
                throw new RuntimeException("Arquivo local não encontrado: " + inputFilePath);
            }
        }
    }

    /**
     * @param path
     * @return
     */
    private static boolean isRemotePath(String path) {
        return path.startsWith("http://") || path.startsWith("https://");
    }

    /**
     * @param urlString
     * @return
     */
    private static boolean urlExists(String urlString) {
        try {
            URI uri = URI.create(urlString);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
        } catch (IOException | IllegalArgumentException e) {
            log.error("Erro ao verificar URL remota: {}", urlString, e);
            return false;
        }
    }

    /**
     * @param dir
     */
    public static void createDirectoryIfNotExists(String dir) {
        File directory = new File(dir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("Diretório criado: {}", dir);
            } else {
                log.error("Erro ao criar diretório: {}", dir);
                throw new RuntimeException("Erro ao criar diretório: " + dir);
            }
        }
    }

    /**
     * @param source
     * @param target
     * @throws IOException
     */
    public static void moveFile(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Arquivo movido de {} para {}", source, target);
    }

    /**
     * @param file
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

    public static String buildFilePath(String directory, String fileName) {
        if (!directory.endsWith(File.separator)) {
            directory += File.separator;
        }

        return directory + fileName;
    }

}
