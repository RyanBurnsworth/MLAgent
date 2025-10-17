package com.ryanburnsworth.mlagent.mlagent.controllers;

import com.ryanburnsworth.mlagent.mlagent.models.ResponseStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface AgentController {
    ResponseEntity<ResponseStatus> startAgents(@RequestParam String notebookName, @RequestParam String searchTerm);
}
