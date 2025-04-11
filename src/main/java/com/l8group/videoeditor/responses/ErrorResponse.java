package com.l8group.videoeditor.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String message;

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message);
    }
}
