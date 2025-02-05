package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "videos_conversion")
public class VideoConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "video_files_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_format", nullable = false)
    private String fileFormat;

    @Column(name = "target_format", nullable = false)
    private String targetFormat;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt; 

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatus status;
    
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
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



    




}
