package com.l8group.videoeditor.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class videoOperationsConverter implements AttributeConverter<List<String>, String> {

    private static final String SPLIT_CHAR = ", ";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return String.join(SPLIT_CHAR, attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(dbData.split(SPLIT_CHAR))
                     .map(String::trim)
                     .collect(Collectors.toList());
    }
}
