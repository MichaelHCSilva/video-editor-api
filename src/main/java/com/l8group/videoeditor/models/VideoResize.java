package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;

import jakarta.persistence.*;

@Entity
@Table(name = "videos_resizes")
public class VideoResize {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "video_file_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private VideoStatusEnum status;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdTimes;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedTimes;

    @Column(name = "target_resolution", nullable = false)
    private String targetResolution;

    @Column(nullable = false)
    private int retryCount = 0;

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

    public VideoStatusEnum getStatus() {
        return status;
    }

    public void setStatus(VideoStatusEnum status) {
        this.status = status;
    }

    public ZonedDateTime getCreatedTimes() {
        return createdTimes;
    }

    public void setCreatedTimes(ZonedDateTime createdTimes) {
        this.createdTimes = createdTimes;
    }

    public ZonedDateTime getUpdatedTimes() {
        return updatedTimes;
    }

    public void setUpdatedTimes(ZonedDateTime updatedTimes) {
        this.updatedTimes = updatedTimes;
    }

    public String getTargetResolution() {
        return targetResolution;
    }

    public void setTargetResolution(String targetResolution) {
        this.targetResolution = targetResolution;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

}
