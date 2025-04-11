package com.l8group.videoeditor.dtos;

import com.l8group.videoeditor.enums.OverlayPositionEnum;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoOverlayResponseDTO {
    private UUID id;
    private String watermark;
    private OverlayPositionEnum position;
    private Integer fontSize;
}