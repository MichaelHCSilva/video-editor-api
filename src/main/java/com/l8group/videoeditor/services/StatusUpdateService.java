package com.l8group.videoeditor.services;

import com.l8group.videoeditor.enums.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class StatusUpdateService {

    @Transactional
    public <T> void updateStatus(JpaRepository<T, UUID> repository, UUID entityId, VideoStatus status) {
        Optional<T> entityOptional = repository.findById(entityId);
        if (entityOptional.isPresent()) {
            T entity = entityOptional.get();

            try {
                updateStatusAndUpdatedAt(entity, status);
                repository.save(entity);
                System.out.println("✅ Status atualizado para " + status + " na entidade com ID: " + entityId);
            } catch (Exception e) {
                System.err.println("❌ Erro ao atualizar status na entidade com ID: " + entityId + ": " + e.getMessage());
                throw new RuntimeException("❌ Erro ao atualizar status na entidade: " + e.getMessage(), e);
            }
        } else {
            System.err.println("⚠️ Nenhuma entidade encontrada com ID: " + entityId);
            throw new RuntimeException("⚠️ Nenhuma entidade encontrada com ID: " + entityId);
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