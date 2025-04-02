/*package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatusEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoStatusService {

    private static final Logger logger = LoggerFactory.getLogger(VideoStatusService.class);

    @Transactional
    public <T> void updateVideoStatus(JpaRepository<T, UUID> repository, UUID entityId, VideoStatusEnum status, String errorSource) {
        Optional<T> entityOptional = repository.findById(entityId);
        if (entityOptional.isPresent()) {
            T entity = entityOptional.get();
            try {
                updateStatusAndTimestamp(entity, status);
                repository.save(entity);
                logger.info("Status atualizado para {} na entidade com ID: {}. Origem: {}", status, entityId, errorSource);
            } catch (NoSuchMethodException e) {
                logger.error("Métodos setStatus ou setUpdatedAt não encontrados na entidade com ID: {}. Origem: {}", entityId, errorSource, e);
                throw new RuntimeException("Métodos setStatus ou setUpdatedAt não encontrados na entidade.", e);
            } catch (InvocationTargetException e) {
                logger.error("Erro ao invocar métodos setStatus ou setUpdatedAt na entidade com ID: {}. Origem: {}", entityId, errorSource, e);
                throw new RuntimeException("Erro ao invocar métodos setStatus ou setUpdatedAt na entidade.", e);
            } catch (Exception e) {
                logger.error("Erro ao atualizar status na entidade com ID: {}. Origem: {}", entityId, errorSource, e);
                throw new RuntimeException("Erro ao atualizar status na entidade.", e);
            }
        } else {
            logger.warn("Nenhuma entidade encontrada com ID: {}. Origem: {}", entityId, errorSource);
            throw new RuntimeException("Nenhuma entidade encontrada com ID: " + entityId);
        }
    }

    private <T> void updateStatusAndTimestamp(T entity, VideoStatusEnum status) throws Exception {
        Method setStatusMethod = findMethod(entity.getClass(), "setStatus", VideoStatusEnum.class);
        Method setUpdatedAtMethod = findMethod(entity.getClass(), "setUpdatedTimes", ZonedDateTime.class);

        if (setStatusMethod != null && setUpdatedAtMethod != null) {
            setStatusMethod.invoke(entity, status);
            setUpdatedAtMethod.invoke(entity, ZonedDateTime.now());
        } else {
            throw new NoSuchMethodException("Métodos setStatus ou setUpdatedAt não encontrados na entidade.");
        }
    }

    private Method findMethod(Class<?> clazz, String methodName, Class<?> parameterType) {
        try {
            return clazz.getMethod(methodName, parameterType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}*/