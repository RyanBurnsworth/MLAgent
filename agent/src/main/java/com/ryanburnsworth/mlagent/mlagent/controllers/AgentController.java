package com.ryanburnsworth.mlagent.mlagent.controllers;

import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface AgentController {
    ResponseEntity<Map<String, Object>> performAction();
}
