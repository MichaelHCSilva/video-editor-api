package com.l8group.videoeditor.services;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.exceptions.VideoNotFoundException;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoFileFinderService {

    private static final Logger log = LoggerFactory.getLogger(VideoFileFinderService.class);
    private final VideoFileRepository videoFileRepository;

    public VideoFile findById(UUID id) {
        log.debug("Buscando vídeo com ID: {}", id);
        return videoFileRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Vídeo não encontrado para o ID: {}", id);
                    return new VideoNotFoundException(id);
                });
    }
}
