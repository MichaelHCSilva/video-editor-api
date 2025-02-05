package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;
import com.l8group.videoeditor.enums.VideoStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "videos_batch_task")
public class VideoBatchTask {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "batch_process_id", nullable = false)
    private VideoBatchProcess batchProcess; 

    @ManyToOne
    @JoinColumn(name = "video_file_id", nullable = false)
    private VideoFile videoFile; 

    @Column(name = "operation_type", nullable = false)
    private String operationType;

    @Column(name = "operation_parameters", columnDefinition = "json")
    private String operationParameters;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatus status;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VideoBatchProcess getBatchProcess() {
        return batchProcess;
    }

    public void setBatchProcess(VideoBatchProcess batchProcess) {
        this.batchProcess = batchProcess;
    }

    public VideoFile getVideoFile() {
        return videoFile;
    }

    public void setVideoFile(VideoFile videoFile) {
        this.videoFile = videoFile;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationParameters() {
        return operationParameters;
    }

    public void setOperationParameters(String operationParameters) {
        this.operationParameters = operationParameters;
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

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
