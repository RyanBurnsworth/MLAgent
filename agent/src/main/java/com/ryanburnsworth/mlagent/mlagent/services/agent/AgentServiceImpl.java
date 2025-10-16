package com.ryanburnsworth.mlagent.mlagent.services.agent;

import com.ryanburnsworth.mlagent.mlagent.enums.AgentState;
import com.ryanburnsworth.mlagent.mlagent.models.AgentMemory;
import com.ryanburnsworth.mlagent.mlagent.models.DatasetResponse;
import com.ryanburnsworth.mlagent.mlagent.models.StatusResponse;
import com.ryanburnsworth.mlagent.mlagent.services.ml.MLService;
import com.ryanburnsworth.mlagent.mlagent.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.ryanburnsworth.mlagent.mlagent.util.Prompts.DATA_LOADING_PROMPT;
import static com.ryanburnsworth.mlagent.mlagent.util.Prompts.ERROR_HANDLING_PROMPT;

@Service
public class AgentServiceImpl implements AgentService {
    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    private final ChatClient chatClient;
    private final MLService mlService;
    private final List<AgentMemory> agentMemories;

    private int errorCounter = 0;

    AgentServiceImpl(ChatClient.Builder chatClientBuilder, MLService mlService) {
        this.chatClient = chatClientBuilder.build();
        this.mlService = mlService;
        this.agentMemories = new ArrayList<>();
    }

    @Override
    public StatusResponse performAgenticMachineLearning(String searchTerm) {
        // download the hottest dataset from Kaggle and it's metadata using a search term
        DatasetResponse datasetResponse = mlService.fetchDatasetMetadata(searchTerm);

        // generate the dataset loading notebook cells from the dataset metadata
        StatusResponse status = this.generateDataLoadingCellsAgent(datasetResponse);

        return this.handleStatusResponse(status);
    }

    private StatusResponse generateDataLoadingCellsAgent(DatasetResponse datasetResponse) {
        Prompt prompt = DATA_LOADING_PROMPT.create(
                Map.of(
                        "title", datasetResponse.getTitle(),
                        "subtitle", datasetResponse.getSubtitle(),
                        "description", datasetResponse.getDescription(),
                        "datasets", datasetResponse.getDatasets()
                )
        );

        try {
            // retrieve the notebook content from the LLM
            String notebookContent = chatClient
                    .prompt(prompt)
                    .call()
                    .content();

            Map<String, Object> payload = handleLLMResponse(prompt.getContents(), notebookContent);

            // TODO: update hardcoded titanic with real notebook name
            return performWorkflow("titanic", payload);

        } catch (Exception e) {
            log.error("Error reading notebook content from LLM {}", e.getMessage());
        }
        return null;
    }

    private StatusResponse errorHandlerAgent(StatusResponse response) {
        log.info("ErrorHandlerAgent: Attempting to fix errors");

        // allow 3 attempts at error handling before quitting
        errorCounter += 1;
        if (errorCounter > 3) {
            return StatusResponse.builder()
                    .status("Failure")
                    .message("Unable to fix errors after 3 attempts")
                    .details(response.getDetails())
                    .build();
        }

        Prompt prompt = ERROR_HANDLING_PROMPT.create(
                Map.of(
                        "userPrompt", agentMemories.get(agentMemories.toArray().length - 1).getUserInput(),
                        "aiResponse", agentMemories.get(agentMemories.toArray().length - 1).getAgentOutput(),
                        "errorMessage", response.getMessage(),
                        "errorDetails", response.getDetails()
                )
        );

        try {
            // retrieve the content from the LLM
            String content = chatClient
                    .prompt(prompt)
                    .call()
                    .content();

            Map<String, Object> payload = handleLLMResponse(prompt.getContents(), content);

            log.info("ErrorHandlerAgent: Received content from LLM: {}", content);

            // TODO: update hardcoded titanic with real notebook name
            StatusResponse isComplete = performWorkflow("titanic", payload);
            return this.handleStatusResponse(isComplete);
        } catch (Exception e) {
            log.error("Error reading content from LLM {}", e.getMessage());
        }
        return null;
    }

    private StatusResponse performWorkflow(String name, Map<String, Object> payload) {
        AgentState currentState = agentMemories.get(agentMemories.toArray().length - 1).getAgentState();

        log.info("Performing workflow on current state: {}", currentState.toString());

        if (Objects.requireNonNull(currentState) == AgentState.GENERATING_DATA_LOADER_CELLS) {
            log.info("Generating Data Loader Notebook Cells");
            return this.mlService.generateDataloaderNotebookCells(name, payload);
        }

        return null;
    }

    private void updateAgentMemory(AgentState state, String userInput, String agentOutput) {
        AgentMemory agentMemory = AgentMemory.builder()
                .agentState(state)
                .userInput(userInput)
                .agentOutput(agentOutput)
                .build();

        this.agentMemories.add(agentMemory);
    }

    private Map<String, Object> handleLLMResponse(String prompt, String notebookContent) {

        // update the agent memory
        updateAgentMemory(AgentState.GENERATING_DATA_LOADER_CELLS, prompt, notebookContent);

        // convert the notebook content into a JSON object
        Map<String, Object> notebookContentMap = Util.getJsonFromContent(notebookContent);

        // wrap and return the notebook content payload
        return Util.getWrappedJsonObject("notebook_content", notebookContentMap);
    }

    private StatusResponse handleStatusResponse(StatusResponse status) {
        if (status == null) {
            log.error("No status response from machine learning service");
            return null;
        }

        if ("success".equals(status.getStatus())) {
            // TODO: Move on to the next step
            return status;
        }

        String message = Optional.ofNullable(status.getMessage()).orElse("Unknown error");
        String details = Optional.ofNullable(status.getDetails()).orElse("");
        log.warn("ML service returned an error: {} {}", message, details);

        // Call the error handler agent to fix the issues and try again
        return errorHandlerAgent(status);
    }
}
