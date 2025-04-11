package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoCutResponseDTO {
    private String duration;
    private ZonedDateTime createdAt;
}