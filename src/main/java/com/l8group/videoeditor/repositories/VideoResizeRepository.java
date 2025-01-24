package com.l8group.videoeditor.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.l8group.videoeditor.models.VideoResize;

public interface VideoResizeRepository  extends JpaRepository <VideoResize, UUID>{


}
