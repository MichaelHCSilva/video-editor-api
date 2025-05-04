package com.l8group.videoeditor.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private List<String> errors;
    private String timestamp;

    public static ErrorResponse of(List<String> errors) {
        return new ErrorResponse(errors, java.time.LocalDateTime.now().toString());
    }
}
