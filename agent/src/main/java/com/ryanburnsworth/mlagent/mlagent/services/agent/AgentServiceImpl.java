package com.ryanburnsworth.mlagent.mlagent.services.agent;

import com.ryanburnsworth.mlagent.mlagent.enums.AgentState;
import com.ryanburnsworth.mlagent.mlagent.models.AgentMemory;
import com.ryanburnsworth.mlagent.mlagent.models.DatasetMetadata;
import com.ryanburnsworth.mlagent.mlagent.models.ResponseStatus;
import com.ryanburnsworth.mlagent.mlagent.services.ml.MLService;
import com.ryanburnsworth.mlagent.mlagent.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ryanburnsworth.mlagent.mlagent.util.Prompts.*;

@Service
public class AgentServiceImpl implements AgentService {
    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    private final ChatClient chatClient;
    private final MLService mlService;

    private List<AgentMemory> agentMemories;

    private int errorCounter = 0;

    AgentServiceImpl(ChatClient.Builder chatClientBuilder, MLService mlService) {
        this.chatClient = chatClientBuilder.build();
        this.mlService = mlService;
    }

    @Override
    public ResponseStatus machineLearningOrchestrator(String notebookName, String searchTerm) {
        // instantiate new memories for each ML orchestrator
        this.agentMemories = new ArrayList<>();

        // reset errorCounter
        this.errorCounter = 0;

        ResponseStatus responseStatus;

        // download the hottest dataset from Kaggle and it's metadata using a search term
        DatasetMetadata datasetMetadata = mlService.fetchDatasetMetadata(searchTerm);

        // create the notebook and load the data
        ResponseStatus createStatus = this.notebookCreatorAgent(notebookName, datasetMetadata);
        responseStatus = this.handleResponseStatus(createStatus);

        if (responseStatus == null || responseStatus.getStatus().equals("failed")) {
            return responseStatus;
        }

        // create the preprocessing cells and update the notebook
        ResponseStatus preprocessorStatus = this.preprocessorAgent();
        responseStatus = this.handleResponseStatus(preprocessorStatus);

        if (responseStatus == null || responseStatus.getStatus().equals("failed")) {
            return responseStatus;
        }

        return responseStatus;
    }

    private ResponseStatus notebookCreatorAgent(String notebook_name, DatasetMetadata datasetMetadata) {
        log.info("NotebookCreatorAgent: Creating Notebook");
        Prompt prompt = DATA_LOADING_PROMPT.create(
                Map.of(
                        "title", datasetMetadata.getTitle(),
                        "subtitle", datasetMetadata.getSubtitle(),
                        "description", datasetMetadata.getDescription(),
                        "datasets", datasetMetadata.getDatasets()
                )
        );

        try {
            log.info("NotebookCreatorAgent; Getting notebook content from LLM");
            Map<String, Object> payload = (Map<String, Object>) getPayloadFromLLM(prompt.getContents(), AgentState.CREATING_NOTEBOOK);

            ResponseStatus creationStatus = performNotebookAction(notebook_name, payload);
            return this.handleResponseStatus(creationStatus);
        } catch (Exception e) {
            log.error("Error reading data loading cells from LLM {}", e.getMessage());
            return getResponseStatusError(e);
        }
    }

    private ResponseStatus preprocessorAgent() {
        log.info("PreprocessingAgent: Generating preprocessing notebook cells");

        String memoryContext = Util.formatAgentMemories(agentMemories);
        String prompt = DATA_PREPROCESSING_PROMPT.render(Map.of(
                "memory", memoryContext
        ));

        try {
            ;
            List<Map<String, Object>> payload = (List<Map<String, Object>>) getPayloadFromLLM(prompt, AgentState.PREPROCESSING_DATA);

            // Pass to workflow
            ResponseStatus updateStatus = performNotebookAction("titanic", payload);
            return this.handleResponseStatus(updateStatus);
        } catch (Exception e) {
            log.error("Error reading preprocessing cells from LLM", e);
            return getResponseStatusError(e);
        }
    }

    private ResponseStatus errorHandlerAgent(ResponseStatus response) {
        log.info("ErrorHandlerAgent: Attempting to fix errors");

        String lastUserPrompt = agentMemories.get(agentMemories.toArray().length - 1).getUserInput();
        String lastAgentOutput = agentMemories.get(agentMemories.toArray().length - 1).getAgentOutput();

        // allow 3 attempts at error handling before quitting
        errorCounter += 1;
        if (errorCounter > 3) {
            return ResponseStatus.builder()
                    .status("Failure")
                    .message("Agent Output: " + lastAgentOutput)
                    .details(response.getDetails())
                    .build();
        }

        Prompt prompt = ERROR_HANDLING_PROMPT.create(
                Map.of(
                        "userPrompt", lastUserPrompt,
                        "aiResponse", lastAgentOutput,
                        "errorMessage", response.getMessage(),
                        "errorDetails", response.getDetails()
                )
        );

        try {
            AgentState currentState = agentMemories.get(agentMemories.toArray().length - 1).getAgentState();

            Object payload = getPayloadFromLLM(prompt.getContents(), currentState);

            ResponseStatus updateStatus = performNotebookAction("titanic", payload);
            return this.handleResponseStatus(updateStatus);
        } catch (Exception e) {
            log.error("Error reading content from LLM {}", e.getMessage());
        }
        return null;
    }

    private Object getPayloadFromLLM(String prompt, AgentState currentState) {
        String notebookContent = chatClient
                .prompt(prompt)
                .call()
                .content();

        if (notebookContent == null || notebookContent.isBlank()) {
            log.error("Empty response from LLM during preprocessing cell generation");
            return ResponseStatus.builder()
                    .status("Failure")
                    .message("Empty response from LLM")
                    .details("")
                    .build();
        }

        // Store to memory
        updateAgentMemory(currentState, prompt, notebookContent);

        // Wrap under correct contract
        if (currentState == AgentState.CREATING_NOTEBOOK) {
            Map<String, Object> payload = Util.getJsonFromContent(notebookContent);

            // if payload is already wrapped, don't wrap twice.
            if (!payload.containsKey("notebook_content"))
                return Util.getWrappedJsonObject("notebook_content", payload);
            return payload;
        } else {
            return Util.getJsonFromListContent(notebookContent);
        }
    }

    private ResponseStatus performNotebookAction(String name, Object payload) {
        AgentState currentState = agentMemories.get(agentMemories.size() - 1).getAgentState();
        log.info("Performing notebook action on current state: {}", currentState);

        if (currentState == null) {
            return ResponseStatus.builder().status("failed").message("No agent state available").build();
        }

        // Creating initial notebook
        if (currentState == AgentState.CREATING_NOTEBOOK && payload instanceof Map) {
            log.info("Creating notebook with data loader cells");
            return this.mlService.createNotebook(name, (Map<String, Object>) payload);
        }

        // Updating notebook
        if (currentState == AgentState.PREPROCESSING_DATA && payload instanceof List) {
            log.info("Updating notebook with preprocessing cells");
            return this.mlService.updateNotebook(name, (List<Map<String, Object>>) payload);
        }

        return getResponseStatusError(new Exception("Error performing notebook action"));
    }

    private void updateAgentMemory(AgentState state, String userInput, String agentOutput) {
        log.info("UpdateAgentMemory: Updating Agent Memory to state: {}", state);
        AgentMemory agentMemory = AgentMemory.builder()
                .agentState(state)
                .userInput(userInput)
                .agentOutput(agentOutput)
                .build();

        this.agentMemories.add(agentMemory);
    }

    private ResponseStatus handleResponseStatus(ResponseStatus status) {
        if (status == null) {
            log.error("No status response from machine learning service");
            return null;
        }

        if ("success".equals(status.getStatus())) {
            log.info("Status Response is successful");
            return status;
        }

        String message = Optional.ofNullable(status.getMessage()).orElse("Unknown error");
        String details = Optional.ofNullable(status.getDetails()).orElse("");
        log.warn("ML service returned an error: {} {}", message, details);

        // Call the error handler agent to fix the issues and try again
        return errorHandlerAgent(status);
    }

    private ResponseStatus getResponseStatusError(Exception e) {
        return ResponseStatus.builder()
                .status("Failure")
                .message("Error reading preprocessing cells from LLM")
                .details(e.getMessage())
                .build();
    }
}
