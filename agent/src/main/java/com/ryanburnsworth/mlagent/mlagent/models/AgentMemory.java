package com.ryanburnsworth.mlagent.mlagent.models;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentMemory {
    String userInput;

    String agentOutput;
}
