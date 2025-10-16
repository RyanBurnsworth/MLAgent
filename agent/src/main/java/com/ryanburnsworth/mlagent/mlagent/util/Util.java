package com.ryanburnsworth.mlagent.mlagent.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class Util {
    public static Map<String, Object> getJsonFromContent(String value) {

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(value, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> getWrappedJsonObject(String key, Object value) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(key, value);

        return payload;
    }
}
