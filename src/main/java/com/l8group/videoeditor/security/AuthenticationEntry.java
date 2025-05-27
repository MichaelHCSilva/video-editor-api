package com.l8group.videoeditor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.l8group.videoeditor.responses.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

@Component
public class AuthenticationEntry implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ErrorResponse errorResponse = new ErrorResponse(Collections.singletonList("Acesso negado. Por favor, forneça um token JWT válido."), LocalDateTime.now().toString());

        try {
            String jsonError = objectMapper.writeValueAsString(errorResponse);
            System.err.println("Resposta de erro JSON (CustomEntryPoint): " + jsonError); 
            response.getOutputStream().println(jsonError);
        } catch (IOException e) {
            e.printStackTrace(); 
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("{\"message\": \"Erro interno ao processar a autenticação.\"}");
        }
    }
}