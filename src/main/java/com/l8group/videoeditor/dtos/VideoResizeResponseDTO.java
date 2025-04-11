package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoResizeResponseDTO {
    private String resolution;
    private ZonedDateTime createdAt;
}