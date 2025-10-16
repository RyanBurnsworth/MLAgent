package com.ryanburnsworth.mlagent.mlagent.controllers;

import com.ryanburnsworth.mlagent.mlagent.models.StatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface AgentController {
    ResponseEntity<StatusResponse> initiateAgents(@RequestParam String searchTerm);
}
