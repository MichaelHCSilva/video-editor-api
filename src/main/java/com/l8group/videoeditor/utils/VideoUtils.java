package com.l8group.videoeditor.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class VideoUtils {

    public static String generateShortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String formatDateToCompactString(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        return date.format(formatter);
    }
}