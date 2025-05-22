package com.l8group.videoeditor.services;

import com.l8group.videoeditor.dtos.UserResponseDto;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.metrics.UserMetrics;
import com.l8group.videoeditor.models.UserAccount;
import com.l8group.videoeditor.repositories.UserRepository;
import com.l8group.videoeditor.requests.UserRequest;
import com.l8group.videoeditor.rabbit.producer.UserStatusProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMetrics userMetrics;
    private final UserStatusProducer userStatusProducer;
    private final VideoStatusManagerService videoStatusManagerService;

    @Transactional
    public UserResponseDto registerUser(UserRequest userRequest) {
        log.info("[{}] Iniciando o registro do usuário: {}", ZonedDateTime.now(), userRequest.getUserName());
        log.debug("[{}] Instância de UserMetrics injetada: {}", ZonedDateTime.now(), userMetrics); 
        UserAccount savedUser = null;

        try {
            if (userRepository.existsByUserName(userRequest.getUserName())) {
                log.warn("[{}] Nome de usuário já existe: {}", ZonedDateTime.now(), userRequest.getUserName());
                userMetrics.incrementRegistrationErrorCount();
                throw new IllegalArgumentException("Nome de usuário já existe.");
            }

            if (userRepository.existsByEmail(userRequest.getEmail())) {
                log.warn("[{}] Email já existe: {}", ZonedDateTime.now(), userRequest.getEmail());
                userMetrics.incrementRegistrationErrorCount();
                throw new IllegalArgumentException("Email já existe.");
            }

            UserAccount newUser = new UserAccount();
            newUser.setUserName(userRequest.getUserName());
            newUser.setEmail(userRequest.getEmail());
            newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
            newUser.setCreatedTimes(ZonedDateTime.now());
            newUser.setUpdatedTimes(ZonedDateTime.now());
            newUser.setStatus(VideoStatusEnum.PROCESSING); // Status inicial
            log.info("[{}] Status inicial do usuário '{}' definido como: {}", ZonedDateTime.now(), newUser.getUserName(), VideoStatusEnum.PROCESSING);

            savedUser = userRepository.save(newUser);
            log.info("[{}] Usuário registrado com sucesso com ID: {}", ZonedDateTime.now(), savedUser.getId());
            userMetrics.incrementRegistrationSuccessCount();

            VideoStatusEnum finalStatus = VideoStatusEnum.COMPLETED;
            userStatusProducer.sendUserStatusUpdate(savedUser.getId(), finalStatus);
            log.info("[{}] Mensagem de atualização de status enviada para o usuário com ID: {} para o status: {}", ZonedDateTime.now(), savedUser.getId(), finalStatus);

            videoStatusManagerService.updateEntityStatus(userRepository, savedUser.getId(), finalStatus, "UserService - Registro");
            log.info("[{}] Status do usuário com ID '{}' atualizado para '{}' via VideoStatusManagerService", ZonedDateTime.now(), savedUser.getId(), finalStatus);

            return new UserResponseDto(
                    savedUser.getUserName(),
                    savedUser.getEmail(),
                    savedUser.getCreatedTimes()
            );

        } catch (IllegalArgumentException e) {
            log.warn("[{}] Erro de argumento ao registrar o usuário '{}': {}", ZonedDateTime.now(), userRequest.getUserName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[{}] Erro inesperado ao registrar o usuário '{}': {}", ZonedDateTime.now(), userRequest.getUserName(), e.getMessage());
            userMetrics.incrementRegistrationErrorCount();
            if (savedUser != null) {
                videoStatusManagerService.updateEntityStatus(userRepository, savedUser.getId(), VideoStatusEnum.ERROR, "UserService - Falha no Registro");
                log.error("[{}] Status do usuário com ID '{}' atualizado para '{}' devido a erro no registro", ZonedDateTime.now(), savedUser.getId(), VideoStatusEnum.ERROR);
            }
            throw new RuntimeException("Erro inesperado durante o registro do usuário.", e);
        }
    }

    public void updateUserStatus(UUID userId, VideoStatusEnum status, String source) {
        log.info("[{}] Recebida solicitação para atualizar o status do usuário com ID '{}' para '{}' (Origem: {})", ZonedDateTime.now(), userId, status, source);
        videoStatusManagerService.updateEntityStatus(userRepository, userId, status, source);
        log.info("[{}] Status do usuário com ID '{}' atualizado para '{}' (Origem: {})", ZonedDateTime.now(), userId, status, source);
    }
}