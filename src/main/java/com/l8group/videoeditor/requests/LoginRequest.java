package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "O nome de usuário é obrigatório.")
    private String userName;

    @NotBlank(message = "A senha é obrigatória.")
    private String password;
}