package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "videos_conversions")
@Data
@NoArgsConstructor
public class VideoConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "video_files_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "video_file_format", nullable = false)
    private String videoFileFormat;

    @Column(name = "video_target_format", nullable = false)
    private String videoTargetFormat;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdTimes;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedTimes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatusEnum status;

    @Column(nullable = false)
    private int retryCount = 0;
}