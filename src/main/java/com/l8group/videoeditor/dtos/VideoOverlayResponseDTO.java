package com.l8group.videoeditor.dtos;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoOverlayResponseDTO {
    private UUID id;
    private String watermark;
    private String position; 
    private Integer fontSize;
}
