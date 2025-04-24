package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "videos_files")
@Data
@NoArgsConstructor
public class VideoFile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "video_file_name", nullable = false)
    private String videoFileName;

    @Column(name = "video_file_size", nullable = false)
    private Long videoFileSize;

    @Column(name = "video_file_format", nullable = false)
    private String videoFileFormat;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdTimes;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedTimes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatusEnum status;

    @Column(name = "video_duration", nullable = false)
    private String videoDuration;

    @Column(name = "video_file_path", nullable = false)
    private String videoFilePath;

    @Column(nullable = false)
    private int retryCount = 0;
}