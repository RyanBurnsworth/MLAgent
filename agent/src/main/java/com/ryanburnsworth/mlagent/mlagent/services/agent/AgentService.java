package com.ryanburnsworth.mlagent.mlagent.services.agent;

import com.ryanburnsworth.mlagent.mlagent.models.StatusResponse;

public interface AgentService {
    StatusResponse performAgenticMachineLearning(String searchTerm);
}
