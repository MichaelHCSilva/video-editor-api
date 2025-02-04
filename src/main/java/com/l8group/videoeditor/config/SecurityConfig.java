package com.l8group.videoeditor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/").permitAll()  
                .requestMatchers("/api/videos").permitAll()
                .requestMatchers("/api/videos/upload").permitAll()
                .requestMatchers("/api/videos/edit/cut").permitAll()
                .requestMatchers("/api/videos/edit/resize").permitAll()
                .requestMatchers("/api/videos/edit/overlay-text").permitAll()
                .requestMatchers("/api/videos/convert").permitAll()
                .requestMatchers("/error").permitAll() 
                .anyRequest().authenticated()  
            );
        return http.build();
    }
}
