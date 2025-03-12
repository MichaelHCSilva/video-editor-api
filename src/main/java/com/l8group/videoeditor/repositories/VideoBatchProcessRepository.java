package com.l8group.videoeditor.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.l8group.videoeditor.models.VideoProcessingBatch;

@Repository
public interface VideoBatchProcessRepository extends JpaRepository<VideoProcessingBatch, UUID> {

}
