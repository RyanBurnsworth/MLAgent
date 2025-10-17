package com.ryanburnsworth.mlagent.mlagent.services.agent;

import com.ryanburnsworth.mlagent.mlagent.models.ResponseStatus;

public interface AgentService {
    ResponseStatus machineLearningOrchestrator(String notebookName, String searchTerm);
}
