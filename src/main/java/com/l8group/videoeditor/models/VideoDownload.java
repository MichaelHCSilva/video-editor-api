package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "videos_download")
@Data
@NoArgsConstructor
public class VideoDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "batch_process_id", nullable = false)
    private VideoProcessingBatch videoProcessingBatch;

    @Column(name = "download_file_name", nullable = false)
    private String downloadFileName;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdTimes;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedTimes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatusEnum status;

    @Column(name = "s3_url", columnDefinition = "TEXT", nullable = true)
    private String s3Url;

    @Column(nullable = false)
    private int retryCount = 0;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount userAccount;
}
