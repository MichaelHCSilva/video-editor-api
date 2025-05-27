package com.l8group.videoeditor.services;

import com.l8group.videoeditor.dtos.UserResponseDto;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.metrics.UserMetrics;
import com.l8group.videoeditor.models.UserAccount;
import com.l8group.videoeditor.rabbit.producer.UserStatusProducer;
import com.l8group.videoeditor.repositories.UserRepository;
import com.l8group.videoeditor.requests.UserRequest;
import com.l8group.videoeditor.validation.UserValidation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserStatusProducer userStatusProducer;
    private final UserMetrics userMetrics;
    private final UserValidation userValidationService;
    private final VideoStatusService videoStatusManagerService;


    @Transactional
    public UserResponseDto registerUser(UserRequest userRequest) {
        UserAccount newUser = new UserAccount();
        try {

            userValidationService.validateUserRequest(userRequest);

            newUser.setUserName(userRequest.getUserName());
            newUser.setEmail(userRequest.getEmail());
            newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
            newUser.setCreatedTimes(ZonedDateTime.now());
            newUser.setUpdatedTimes(ZonedDateTime.now());
            newUser.setStatus(VideoStatusEnum.PROCESSING);
            newUser.setRetryCount(0);

            UserAccount savedUser = userRepository.save(newUser);

            videoStatusManagerService.updateEntityStatus(userRepository, savedUser.getId(), VideoStatusEnum.COMPLETED,
                    "UserService.registerUser");

            userStatusProducer.sendUserStatusUpdate(savedUser.getId(), VideoStatusEnum.COMPLETED);

            userMetrics.incrementRegistrationSuccessCount();

            return mapToDto(savedUser);

        } catch (Exception e) {
            userMetrics.incrementRegistrationErrorCount();
            log.error("Erro ao registrar usuário: {}", e.getMessage(), e);

            if (newUser.getId() != null) {
                videoStatusManagerService.updateEntityStatus(userRepository, newUser.getId(), VideoStatusEnum.ERROR,
                        "UserService.registerUser");
            }

            throw e;
        }
    }

    private UserResponseDto mapToDto(UserAccount user) {
        return new UserResponseDto(
                user.getUserName(),
                user.getEmail(),
                user.getCreatedTimes());
    }
}
