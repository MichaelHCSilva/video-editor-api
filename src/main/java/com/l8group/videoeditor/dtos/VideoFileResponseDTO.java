package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoFileResponseDTO {
    private UUID id;
    private String fileName;
    private ZonedDateTime createdAt;
}