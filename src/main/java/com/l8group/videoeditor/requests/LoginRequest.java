package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Informe o nome de usuário para acessar a conta, é obrigatório e não pode estar em branco.")
    //@Pattern(regexp = "^[a-zA-Z\\s]+$", message = "O nome de usuário só pode conter letras.")
    //@Size(max = 50, message = "O nome de usuário pode ter no máximo 50 caracteres.")
    private String userName;

    @NotBlank(message = "Informe sua senha para prosseguir com o login.")
    //@Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres.")
    private String password;
}