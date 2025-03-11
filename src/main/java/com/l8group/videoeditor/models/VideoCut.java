package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "videos_cut") 
public class VideoCut {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "video_file_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "duration", nullable = false)
    private String duration; 

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

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

    public String getDuration() {  
        return duration;
    }

    public void setDuration(String duration) {  
        this.duration = duration;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
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









    
}