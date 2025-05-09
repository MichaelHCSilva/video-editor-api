package com.l8group.videoeditor.requests;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserRequest {

    @NotBlank(message = "O nome de usuário é obrigatório.")
    @Size(min = 3, max = 50, message = "O nome de usuário deve ter entre 3 e 50 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "O nome de usuário deve conter apenas letras, números e underscores.")
    private String userName;

    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "Formato de email inválido.")
    @Size(max = 100, message = "O email não pode ter mais de 100 caracteres.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres.")
    private String password;


}