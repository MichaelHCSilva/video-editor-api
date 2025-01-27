package com.l8group.videoeditor.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.l8group.videoeditor.enums.VideoResolution;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.models.VideoResize;

@Repository
public interface VideoResizeRepository extends JpaRepository<VideoResize, UUID> {

    Optional<VideoResize> findByVideoFileAndResolution(VideoFile videoFile, VideoResolution resolution);
}
