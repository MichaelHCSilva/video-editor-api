package com.l8group.videoeditor.services;

import com.l8group.videoeditor.exceptions.VideoProcessingNotFoundException;
import com.l8group.videoeditor.models.VideoProcessingBatch;
import com.l8group.videoeditor.repositories.VideoBatchProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoProcessingBatchFinderService {

    private final VideoBatchProcessRepository videoBatchProcessRepository;

    public VideoProcessingBatch findById(UUID id) {
        return videoBatchProcessRepository.findById(id)
                .orElseThrow(() -> new VideoProcessingNotFoundException(
                        String.format("Nenhum processamento encontrado correspondente ao ID: '%s'", id)
                ));
    }

    public VideoProcessingBatch findById(String rawId) {
        UUID id;
        try {
            id = UUID.fromString(rawId);
        } catch (IllegalArgumentException ex) {
            throw new VideoProcessingNotFoundException(
                    String.format("Nenhum processamento encontrado correspondente ao ID: '%s'", rawId)
            );
        }
        return findById(id);
    }
}