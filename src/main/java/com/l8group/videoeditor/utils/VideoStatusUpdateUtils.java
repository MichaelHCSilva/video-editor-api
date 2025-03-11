package com.l8group.videoeditor.utils;

import com.l8group.videoeditor.enums.VideoStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoStatusUpdateUtils {

    private static final Logger logger = LoggerFactory.getLogger(VideoStatusUpdateUtils.class);

    @Transactional
    public <T> void updateStatus(JpaRepository<T, UUID> repository, UUID entityId, VideoStatus status) {
        Optional<T> entityOptional = repository.findById(entityId);
        if (entityOptional.isPresent()) {
            T entity = entityOptional.get();
            try {
                updateStatusAndUpdatedAt(entity, status);
                repository.save(entity);
                logger.info("Status atualizado para {} na entidade com ID: {}", status, entityId);
            } catch (NoSuchMethodException e) {
                logger.error("Métodos setStatus ou setUpdatedAt não encontrados na entidade com ID: {}", entityId, e);
                throw new RuntimeException("Métodos setStatus ou setUpdatedAt não encontrados na entidade.", e);
            } catch (Exception e) {
                logger.error("Erro ao atualizar status na entidade com ID: {}", entityId, e);
                throw new RuntimeException("Erro ao atualizar status na entidade.", e);
            }
        } else {
            logger.warn("Nenhuma entidade encontrada com ID: {}", entityId);
            throw new RuntimeException("Nenhuma entidade encontrada com ID: " + entityId);
        }
    }

    private <T> void updateStatusAndUpdatedAt(T entity, VideoStatus status) throws Exception {
        Method setStatusMethod = findMethod(entity.getClass(), "setStatus", VideoStatus.class);
        Method setUpdatedAtMethod = findMethod(entity.getClass(), "setUpdatedAt", ZonedDateTime.class);

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
}