package com.l8group.videoeditor.controllers;

import com.l8group.videoeditor.dtos.AuthenticationResponse;
import com.l8group.videoeditor.dtos.UserResponseDto;
import com.l8group.videoeditor.requests.LoginRequest;
import com.l8group.videoeditor.requests.UserRequest;
import com.l8group.videoeditor.responses.ErrorResponse;
import com.l8group.videoeditor.services.JwtService;
import com.l8group.videoeditor.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService; 
    private final JwtService jwtService; 
    private final AuthenticationManager authenticationManager; 

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> registerUser(@Valid @RequestBody UserRequest userRequest) {
        log.info("Recebida requisição de registro de usuário: {}", userRequest);
        UserResponseDto userResponse = userService.registerUser(userRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUserName(), loginRequest.getPassword()));

            String jwtToken = jwtService.generateToken(authentication.getName());
            return ResponseEntity.ok(new AuthenticationResponse(jwtToken));

        } catch (AuthenticationException e) {
            log.warn("Falha na autenticação para o usuário: {}", loginRequest.getUserName(), e);
            
            return new ResponseEntity<ErrorResponse>( 
                    new ErrorResponse(
                            Collections.singletonList("Credenciais inválidas. Por favor, verifique seu nome de usuário e senha."),
                            java.time.LocalDateTime.now().toString()
                    ),
                    HttpStatus.UNAUTHORIZED
            );
        }
    }
}