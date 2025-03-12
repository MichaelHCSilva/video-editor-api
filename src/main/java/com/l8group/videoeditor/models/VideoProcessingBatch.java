package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.utils.OperationsConverter;

import jakarta.persistence.*;

@Entity
@Table(name = "video_processing_batches")
public class VideoProcessingBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "video_file_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "video_output_filename", nullable = false)
    private String videoOutputFileName;

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

    public String getVideoOutputFileName() {
        return videoOutputFileName;
    }

    public void setVideoOutputFileName(String videoOutputFileName) {
        this.videoOutputFileName = videoOutputFileName;
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

}
