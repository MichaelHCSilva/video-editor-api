package com.l8group.videoeditor.dto;

import java.util.UUID;

public class VideoEditDTO {

    private UUID id;
    private UUID videoId;
    private Long duration; // Duração do vídeo
    private String name;   // Nome do vídeo editado

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
