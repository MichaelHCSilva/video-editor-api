package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.OverlayPosition;
import com.l8group.videoeditor.enums.VideoStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatus status;

    @Column(name = "overlay_data", columnDefinition = "TEXT", nullable = false)
    private String overlayData;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false)
    private OverlayPosition overlayPosition;

    @Column(name = "font_size", nullable = false)
    private Integer fontSize;  

    @Column(name = "file_path")  
    private String filePath;

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

    public VideoStatus getStatus() {
        return status;
    }

    public void setStatus(VideoStatus status) {
        this.status = status;
    }

    public String getOverlayData() {
        return overlayData;
    }

    public void setOverlayData(String overlayData) {
        this.overlayData = overlayData;
    }

    public OverlayPosition getOverlayPosition() {
        return overlayPosition;
    }

    public void setOverlayPosition(OverlayPosition overlayPosition) {
        this.overlayPosition = overlayPosition;
    }

    public int getFontSize() {  // Método getter para o tamanho da fonte
        return fontSize;
    }

    public void setFontSize(int fontSize) {  // Método setter para o tamanho da fonte
        this.fontSize = fontSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
