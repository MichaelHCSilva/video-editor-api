package com.l8group.videoeditor.requests;

import java.time.Duration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.l8group.videoeditor.utils.VideoDurationUtils;

public class VideoCutRequest {

    @NotBlank(message = "O ID do vídeo é obrigatório.")
    private String videoId;

    @NotBlank(message = "O tempo de início é obrigatório.")
    @Pattern(regexp = "\\d{2}:\\d{2}:\\d{2}", message = "O horário de início deve estar no formato HH:mm:ss.")
    private String startTime; 

    @NotBlank(message = "O tempo de término é obrigatório.")
    @Pattern(regexp = "\\d{2}:\\d{2}:\\d{2}", message = "O horário de término deve estar no formato HH:mm:ss.")
    private String endTime; 

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public Duration getStartTime() {
        return VideoDurationUtils.convertToDuration(startTime);
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public Duration getEndTime() {
        return VideoDurationUtils.convertToDuration(endTime);
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
