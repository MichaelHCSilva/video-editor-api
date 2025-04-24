package com.l8group.videoeditor.dtos;

import java.time.ZonedDateTime;
import com.l8group.videoeditor.enums.VideoStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoFileListDTO {
    private String fileName;
    private ZonedDateTime createdAt;
    private VideoStatusEnum status;
}