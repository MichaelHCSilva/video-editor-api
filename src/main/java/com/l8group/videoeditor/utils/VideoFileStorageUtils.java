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

    /**
     * Resolve o caminho do arquivo, priorizando o anterior se fornecido.
     */
    public static String resolveInputFilePath(String previousFilePath, String defaultPath) {
        String path = (previousFilePath != null && !previousFilePath.isEmpty())
                ? previousFilePath
                : defaultPath;
        log.info("Arquivo de entrada definido como: {}", path);
        return path;
    }

    /**
     * Valida se o arquivo de entrada realmente existe.
     * 
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
     * Verifica se o caminho é um URL remoto.
     * 
     * @param path Caminho a ser verificado.
     * @return Retorna verdadeiro se for um URL, falso caso contrário.
     */
    private static boolean isRemotePath(String path) {
        return path.startsWith("http://") || path.startsWith("https://");
    }

    /**
     * Verifica se a URL está acessível.
     * 
     * @param urlString URL a ser verificada.
     * @return Retorna verdadeiro se a URL for acessível, falso caso contrário.
     */
    private static boolean urlExists(String urlString) {
        try {
            URI uri = URI.create(urlString);
            URL url = uri.toURL(); // uso não-deprecated de URL
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
     * Cria um diretório se ele não existir.
     * 
     * @param dir Caminho do diretório a ser criado.
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
     * Move um arquivo de um local para outro.
     * 
     * @param source Caminho do arquivo de origem.
     * @param target Caminho do arquivo de destino.
     * @throws IOException Se ocorrer um erro durante a movimentação.
     */
    public static void moveFile(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Arquivo movido de {} para {}", source, target);
    }

    /**
     * Exclui um arquivo se ele existir.
     * 
     * @param file O arquivo a ser excluído.
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
        // Verifica se o diretório não termina com "/" e adiciona se necessário
        if (!directory.endsWith(File.separator)) {
            directory += File.separator;
        }

        // Retorna o caminho completo do arquivo
        return directory + fileName;
    }

}
