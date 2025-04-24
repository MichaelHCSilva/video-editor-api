package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoBatchResponseDTO {
    private UUID videoId;
    private String fileName;
    private ZonedDateTime createdAt;
    private List<String> operations;
}