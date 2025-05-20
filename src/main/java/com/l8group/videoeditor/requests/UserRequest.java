package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserRequest {

    @NotBlank(message = "O nome de usuário é obrigatório e não pode estar em branco.")
    @Size(min = 3, max = 100, message = "O nome de usuário precisa ter entre 3 e 100 caracteres.")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "O nome de usuário só pode conter letras.")
    private String userName;

    @NotBlank(message = "O e-mail é obrigatório e não pode estar em branco.")
    @Email(message = "Informe um e-mail válido. Exemplo: usuario@exemplo.com")
    @Size(max = 100, message = "O e-mail pode ter no máximo 100 caracteres.")
    private String email;

    @NotBlank(message = "A senha é obrigatório e não pode estar em branco.")
    @Size(min = 6, max = 20, message = "A senha deve ter no mínimo 6 e no máximo 20 caracteres.")
    private String password;

}