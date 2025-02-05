package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

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
@Table(name = "videos_batch_process")
public class VideoBatchProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
     
    @ManyToOne
    @JoinColumn(name = "video_file_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private VideoStatus status;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime aupdatedAt;

    @Column(columnDefinition = "json")
    private List<String> operations;

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

    public VideoStatus getStatus() {
        return status;
    }

    public void setStatus(VideoStatus status) {
        this.status = status;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getAupdatedAt() {
        return aupdatedAt;
    }

    public void setAupdatedAt(ZonedDateTime aupdatedAt) {
        this.aupdatedAt = aupdatedAt;
    }

    public List<String> getOperations() {
        return operations;
    }

    public void setOperations(List<String> operations) {
        this.operations = operations;
    }



}
