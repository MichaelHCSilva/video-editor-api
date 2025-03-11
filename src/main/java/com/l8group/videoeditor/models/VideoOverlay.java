package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.OverlayPosition;
import com.l8group.videoeditor.enums.VideoStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "videos_overlay")
public class VideoOverlay {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "video_files_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatus status;

    @Column(name = "watermark", columnDefinition = "TEXT", nullable = false)
    private String watermark;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false)
    private OverlayPosition overlayPosition;

    @Column(name = "font_size", nullable = false)
    private Integer fontSize;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VideoFile getVideoFile() {
        return videoFile;
    }

    public void setVideoFile(VideoFile videoFile) {
        this.videoFile = videoFile;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public VideoStatus getStatus() {
        return status;
    }

    public void setStatus(VideoStatus status) {
        this.status = status;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public OverlayPosition getOverlayPosition() {
        return overlayPosition;
    }

    public void setOverlayPosition(OverlayPosition overlayPosition) {
        this.overlayPosition = overlayPosition;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

}
