package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.utils.OperationsConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "video_processing_batches")
public class VideoProcessingBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "video_file_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "video_file_path", nullable = true)
    private String videoFilePath;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private VideoStatusEnum status;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdTimes;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedTimes;

    @Column(name = "processing_steps", nullable = false)
    @Convert(converter = OperationsConverter.class)
    private List<String> processingSteps;

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

    public String getVideoFilePath() {
        return videoFilePath;
    }

    public void setVideoFilePath(String videoFilePath) {
        this.videoFilePath = videoFilePath;
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

    public List<String> getProcessingSteps() {
        return processingSteps;
    }
      
    public void setProcessingSteps(List<String> processingSteps) {
        this.processingSteps = processingSteps;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

}
