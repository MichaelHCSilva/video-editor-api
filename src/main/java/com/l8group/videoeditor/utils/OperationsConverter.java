package com.l8group.videoeditor.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter(autoApply = true)
public class OperationsConverter implements AttributeConverter<List<String>, String> {

    private static final String SEPARATOR = " - ";

    @Override
    public String convertToDatabaseColumn(List<String> operations) {
        if (operations == null || operations.isEmpty()) {
            return "";
        }
        return operations.stream().collect(Collectors.joining(SEPARATOR));
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        return Arrays.asList(dbData.split(SEPARATOR));
    }
}
