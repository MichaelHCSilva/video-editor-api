package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;

import jakarta.persistence.*;

@Entity
@Table(name = "videos_files")
public class VideoFile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "video_file_name", nullable = false)
    private String videoFileName;

    @Column(name = "video_file_size", nullable = false)
    private Long videoFileSize;

    @Column(name = "video_file_format", nullable = false)
    private String videoFileFormat;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdTimes;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedTimes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatusEnum status;

    @Column(name = "video_duration", nullable = false)
    private String videoDuration;

    @Column(name = "video_file_path", nullable = false)
    private String videoFilePath;

    @Column(nullable = false)
    private int retryCount = 0;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVideoFileName() {
        return videoFileName;
    }

    public void setVideoFileName(String videoFileName) {
        this.videoFileName = videoFileName;
    }

    public Long getVideoFileSize() {
        return videoFileSize;
    }

    public void setVideoFileSize(Long videoFileSize) {
        this.videoFileSize = videoFileSize;
    }

    public String getVideoFileFormat() {
        return videoFileFormat;
    }

    public void setVideoFileFormat(String videoFileFormat) {
        this.videoFileFormat = videoFileFormat;
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

    public VideoStatusEnum getStatus() {
        return status;
    }

    public void setStatus(VideoStatusEnum status) {
        this.status = status;
    }

    public String getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(String videoDuration) {
        this.videoDuration = videoDuration;
    }

    public String getVideoFilePath() {
        return videoFilePath;
    }

    public void setVideoFilePath(String videoFilePath) {
        this.videoFilePath = videoFilePath;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

}