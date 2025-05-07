package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.exceptions.InvalidRequestException;
import com.l8group.videoeditor.models.VideoProcessingBatch;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class VideoDownloadValidator {

    public void validateRawBatchProcessId(String rawBatchProcessId) {
        if (!StringUtils.hasText(rawBatchProcessId)) {
            throw new InvalidRequestException("O ID do processo em lote não pode ser nulo ou vazio.");
        }
        try {
            UUID.fromString(rawBatchProcessId);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("O ID do processo em lote fornecido não é um UUID válido.");
        }
    }

    public void validateVideoProcessingBatch(VideoProcessingBatch batch) {
        if (batch == null || !StringUtils.hasText(batch.getVideoFilePath())) {
            throw new InvalidRequestException("O caminho do arquivo de vídeo não foi encontrado para este processo.");
        }
        
    }
}