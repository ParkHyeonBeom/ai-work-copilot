package com.workcopilot.user.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter
public class UserSettingsConverter implements AttributeConverter<UserSettings, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(UserSettings settings) {
        if (settings == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            log.error("UserSettings 직렬화 실패", e);
            throw new IllegalArgumentException("UserSettings 직렬화 실패", e);
        }
    }

    @Override
    public UserSettings convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return UserSettings.defaults();
        }
        try {
            return objectMapper.readValue(json, UserSettings.class);
        } catch (JsonProcessingException e) {
            log.error("UserSettings 역직렬화 실패", e);
            return UserSettings.defaults();
        }
    }
}
