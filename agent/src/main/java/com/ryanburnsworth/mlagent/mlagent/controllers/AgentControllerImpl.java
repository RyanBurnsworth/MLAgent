package com.ryanburnsworth.mlagent.mlagent.controllers;

import com.ryanburnsworth.mlagent.mlagent.services.agent.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController()
public class AgentControllerImpl implements AgentController {
    private final AgentService agentService;

    public AgentControllerImpl(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    @GetMapping("/initialize")
    public ResponseEntity<String> initiateAgents(@RequestParam(name = "searchTerm") String searchTerm) {
        String response = this.agentService.performAgenticMachineLearning(searchTerm);
        return ResponseEntity.ok(response);
    }
}
