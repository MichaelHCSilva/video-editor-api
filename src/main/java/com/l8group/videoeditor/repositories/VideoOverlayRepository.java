package com.l8group.videoeditor.repositories;

import com.l8group.videoeditor.models.VideoOverlay;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import org.springframework.stereotype.Repository;

@Repository
public interface VideoOverlayRepository extends JpaRepository<VideoOverlay, UUID> {
}
