package com.l8group.videoeditor.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "videos_cuts")
@Data
@NoArgsConstructor
public class VideoCut {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "video_file_id", nullable = false)
    private VideoFile videoFile;

    @Column(name = "video_cut_duration", nullable = false)
    private String videoCutDuration;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedTimes;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdTimes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatusEnum status;

    @Column(nullable = false)
    private int retryCount = 0;
}