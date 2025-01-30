package com.l8group.videoeditor.repositories;

import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoResize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoResizeRepository extends JpaRepository<VideoResize, Long> {
    Optional<VideoResize> findByVideoFileAndResolution(VideoFile videoFile, String resolution);
}
