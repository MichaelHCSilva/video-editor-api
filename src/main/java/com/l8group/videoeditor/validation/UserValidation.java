package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.metrics.UserMetrics;
import com.l8group.videoeditor.repositories.UserRepository;
import com.l8group.videoeditor.requests.UserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserValidation {

    private final UserRepository userRepository;
    private final UserMetrics userMetrics;

    public void validateUserRequest(UserRequest userRequest) {
        validateUserName(userRequest.getUserName());
        validateEmail(userRequest.getEmail());
    }

    private void validateUserName(String userName) {
        if (userRepository.existsByUserName(userName)) {
            log.warn("[{}] Nome de usuário já existe: {}", ZonedDateTime.now(), userName);
            userMetrics.incrementRegistrationErrorCount();
            throw new IllegalArgumentException("Nome de usuário já existe.");
        }
    }

    private void validateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("[{}] Email já existe: {}", ZonedDateTime.now(), email);
            userMetrics.incrementRegistrationErrorCount();
            throw new IllegalArgumentException("Email já existe.");
        }
    }
}
