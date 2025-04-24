package com.l8group.videoeditor.repositories;

import com.l8group.videoeditor.models.VideoConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoConversionRepository extends JpaRepository<VideoConversion, UUID> {

Optional<VideoConversion> findByVideoFileIdAndVideoTargetFormat(UUID videoFileId, String videoTargetFormat);
}