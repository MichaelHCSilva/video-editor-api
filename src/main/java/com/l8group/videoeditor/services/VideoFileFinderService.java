package com.l8group.videoeditor.services;

import com.l8group.videoeditor.exceptions.VideoProcessingException;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoFileFinderService {

    private final VideoFileRepository videoFileRepository;

    public VideoFile findById(UUID id) {
        return videoFileRepository.findById(id)
                .orElseThrow(() -> new VideoProcessingException(
                        String.format("Nenhum arquivo de vídeo encontrado correspondente ao ID: '%s'.", id)
                ));
    }
}
