package com.ryanburnsworth.mlagent.mlagent.models;

import com.ryanburnsworth.mlagent.mlagent.enums.AgentState;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentMemory {
    String userInput;

    String agentOutput;

    AgentState agentState;
}
