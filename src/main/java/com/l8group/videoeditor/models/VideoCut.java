package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;

import jakarta.persistence.*;

@Entity
@Table(name = "videos_cuts")
public class VideoCut {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "video_file_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "video_cut_duration", nullable = false)
    private String videoCutDuration;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedTimes;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdTimes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatusEnum status;

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

    public String getVideoCutDuration() {
        return videoCutDuration;
    }

    public void setVideoCutDuration(String videoCutDuration) {
        this.videoCutDuration = videoCutDuration;
    }

    public ZonedDateTime getUpdatedTimes() {
        return updatedTimes;
    }

    public void setUpdatedTimes(ZonedDateTime updatedTimes) {
        this.updatedTimes = updatedTimes;
    }

    public ZonedDateTime getCreatedTimes() {
        return createdTimes;
    }

    public void setCreatedTimes(ZonedDateTime createdTimes) {
        this.createdTimes = createdTimes;
    }

    public VideoStatusEnum getStatus() {
        return status;
    }

    public void setStatus(VideoStatusEnum status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

}