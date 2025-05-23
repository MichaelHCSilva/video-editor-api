package com.l8group.videoeditor.repositories;

import com.l8group.videoeditor.models.VideoDownload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoDownloadRepository extends JpaRepository<VideoDownload, UUID> {
}