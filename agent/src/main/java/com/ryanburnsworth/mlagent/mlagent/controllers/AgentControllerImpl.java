package com.ryanburnsworth.mlagent.mlagent.controllers;

import com.ryanburnsworth.mlagent.mlagent.services.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController()
public class AgentControllerImpl implements AgentController {
    private final AgentService agentService;

    public AgentControllerImpl(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    @GetMapping("/step-one")
    public ResponseEntity<Map<String, Object>> performAction() {
        Map<String, Object> response = this.agentService.dataLoaderAgent("Titanic", "The mystery of the Titanic",
                "How likely were you to die on the titanic", "./datasets/heptapod/titanic/train_and_test2.csv");

        return ResponseEntity.ok(response);
    }
}
