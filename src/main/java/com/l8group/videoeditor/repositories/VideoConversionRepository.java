package com.l8group.videoeditor.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.l8group.videoeditor.models.VideoConversion;

public interface VideoConversionRepository extends JpaRepository<VideoConversion, UUID> {
    Optional<VideoConversion> findByVideoFileId(UUID videoFileId);

    Optional<VideoConversion> findByVideoFileIdAndTargetFormat(UUID videoFileId, String targetFormat);

}
