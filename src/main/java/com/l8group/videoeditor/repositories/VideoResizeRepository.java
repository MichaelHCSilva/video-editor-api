package com.l8group.videoeditor.repositories;

import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoResize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VideoResizeRepository extends JpaRepository<VideoResize, UUID> {
    Optional<VideoResize> findByVideoFileAndTargetResolution(VideoFile videoFile, String targetResolution);
}
