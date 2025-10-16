package com.ryanburnsworth.mlagent.mlagent.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface AgentController {
    ResponseEntity<String> initiateAgents(@RequestParam String searchTerm);
}
