package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.OverlayPositionEnum;
import com.l8group.videoeditor.enums.VideoStatusEnum;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "videos_overlay")
@Data
@NoArgsConstructor
public class VideoOverlay {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "video_files_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdTimes;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedTimes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatusEnum status;

    @Column(name = "overlay_text", columnDefinition = "TEXT", nullable = false)
    private String overlayText;

    @Enumerated(EnumType.STRING)
    @Column(name = "overlay_position", nullable = false)
    private OverlayPositionEnum overlayPosition;

    @Column(name = "overlay_font_size", nullable = false)
    private Integer overlayFontSize;

    @Column(nullable = false)
    private int retryCount = 0;
}