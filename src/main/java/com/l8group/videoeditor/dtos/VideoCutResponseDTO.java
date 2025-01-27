package com.l8group.videoeditor.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VideoCutResponseDTO {
    @NotBlank(message = "O ID do vídeo é obrigatório.")
    private String videoId;

    @NotBlank(message = "O horário de início é obrigatório.")
    @Pattern(regexp = "\\d{2}:\\d{2}:\\d{2}", message = "O horário de início deve estar no formato HH:mm:ss.")
    private String startTime;

    @NotBlank(message = "O horário de término é obrigatório.")
    @Pattern(regexp = "\\d{2}:\\d{2}:\\d{2}", message = "O horário de término deve estar no formato HH:mm:ss.")
    private String endTime;

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

}
