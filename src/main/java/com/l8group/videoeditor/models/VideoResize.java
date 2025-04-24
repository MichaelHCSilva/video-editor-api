package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "videos_resizes")
@Data
@NoArgsConstructor
public class VideoResize {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "video_file_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private VideoStatusEnum status;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdTimes;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedTimes;

    @Column(name = "target_resolution", nullable = false)
    private String targetResolution;

    @Column(nullable = false)
    private int retryCount = 0;
}