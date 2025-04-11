package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoConversionsDTO {
    private String inputFormat;
    private String outputFormat;
    private ZonedDateTime createdAt;
}