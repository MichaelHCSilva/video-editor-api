package com.l8group.videoeditor.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.l8group.videoeditor.enums.VideoStatusEnum;

@Entity
@Table(name = "users", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

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