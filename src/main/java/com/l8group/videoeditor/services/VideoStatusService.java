package com.l8group.videoeditor.services;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import com.l8group.videoeditor.enums.VideoStatusEnum;

import jakarta.transaction.Transactional;

@Service
public class VideoStatusService {

    private static final Logger logger = LoggerFactory.getLogger(VideoStatusService.class);

    @Value("${video.retry.max-attempts}")
    private int maxRetries;

    public VideoStatusService() {
    }

    @Transactional
    public <T> void updateEntityStatus(JpaRepository<T, UUID> repository, UUID entityId, VideoStatusEnum status, String errorSource) {
        try {
            Optional<T> entityOptional = repository.findById(entityId);

            if (entityOptional.isPresent()) {
                T entity = entityOptional.get();

                if (status == VideoStatusEnum.ERROR) {
                    try {
                        int currentRetryCount = (int) entity.getClass().getMethod("getRetryCount").invoke(entity);
                        currentRetryCount++;
                        entity.getClass().getMethod("setRetryCount", int.class).invoke(entity, currentRetryCount);

                        if (currentRetryCount >= maxRetries) {
                            entity.getClass().getMethod("setStatus", VideoStatusEnum.class).invoke(entity, VideoStatusEnum.FAILED_PERMANENTLY);
                            entity.getClass().getMethod("setUpdatedTimes", ZonedDateTime.class).invoke(entity, ZonedDateTime.now());
                            repository.save(entity);
                            logger.warn("Entidade {} atingiu o limite de {} tentativas e foi marcada como FAILED_PERMANENTLY. Origem: {}", entityId, maxRetries, errorSource);
                        } else {
                            entity.getClass().getMethod("setStatus", VideoStatusEnum.class).invoke(entity, status);
                            entity.getClass().getMethod("setUpdatedTimes", ZonedDateTime.class).invoke(entity, ZonedDateTime.now());
                            repository.save(entity);
                            logger.info("Status da entidade {} atualizado para {}. Origem: {}. Tentativa: {}", entityId, status, errorSource, currentRetryCount);
                        }
                    } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                        logger.error("A entidade não possui os métodos getRetryCount, setRetryCount, setStatus ou setUpdatedTimes: {}", e.getMessage());
                    }
                } else if (status == VideoStatusEnum.COMPLETED) {
                    try {
                        entity.getClass().getMethod("setStatus", VideoStatusEnum.class).invoke(entity, status);
                        entity.getClass().getMethod("setUpdatedTimes", ZonedDateTime.class).invoke(entity, ZonedDateTime.now());
                        entity.getClass().getMethod("setRetryCount", int.class).invoke(entity, 0); // Reset retryCount
                        repository.save(entity);
                        logger.info("Status da entidade {} atualizado para {}. Origem: {}", entityId, status, errorSource);
                    } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                        logger.error("A entidade não possui os métodos setStatus, setUpdatedTimes ou setRetryCount: {}", e.getMessage());
                    }
                }
            } else {
                logger.error("Entidade {} não encontrada para atualizar o status. Origem: {}", entityId, errorSource);
            }
        } catch (Exception e) {
            logger.error("Erro ao atualizar o status da entidade {}: {}. Origem: {}", entityId, e.getMessage(), errorSource);
        }
    }
}