package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.utils.videoOperationsConverter;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "videos_processing_batches")
@Data
@NoArgsConstructor
public class VideoProcessingBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "video_file_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "s3_url", columnDefinition = "TEXT", nullable = true)
    private String s3Url;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private VideoStatusEnum status;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdTimes;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedTimes;

    @Column(name = "processing_steps", nullable = false)
    @Convert(converter = videoOperationsConverter.class)
    private List<String> processingSteps;

    @Column(nullable = false)
    private int retryCount = 0;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount userAccount;
}