package com.l8group.videoeditor.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SuccessResponse {
    private String message;

    public static SuccessResponse of(String message) {
        return new SuccessResponse(message);
    }
}
