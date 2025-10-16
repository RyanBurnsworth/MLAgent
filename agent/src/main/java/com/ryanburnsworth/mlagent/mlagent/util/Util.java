package com.ryanburnsworth.mlagent.mlagent.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryanburnsworth.mlagent.mlagent.models.AgentMemory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Util {
    public static Map<String, Object> getJsonFromContent(String value) {

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(value, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Map<String, Object>> getJsonFromListContent(String value) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(
                    value,
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> getWrappedJsonObject(String key, Object value) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(key, value);

        return payload;
    }

    public static String formatAgentMemories(List<AgentMemory> memories) {
        return memories.stream()
                .map(m -> String.format(
                        "{ \"userInput\": \"%s\", \"agentOutput\": \"%s\" }",
                        escapeForJson(m.getUserInput()),
                        escapeForJson(m.getAgentOutput())
                ))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String escapeForJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
