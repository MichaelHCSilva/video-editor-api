package com.l8group.videoeditor.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private String userName;
    private String email;
    private ZonedDateTime createdTimes;
   
}