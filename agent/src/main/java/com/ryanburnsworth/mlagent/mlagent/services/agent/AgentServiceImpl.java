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
    public StatusResponse performAgenticMachineLearning(String searchTerm) {
        // instantiate new memories for each ML agent
        this.agentMemories = new ArrayList<>();

        // reset errorCounter
        this.errorCounter = 0;

        StatusResponse responseStatus;

        // download the hottest dataset from Kaggle and it's metadata using a search term
        DatasetResponse datasetResponse = mlService.fetchDatasetMetadata(searchTerm);

        // generate the dataset loading notebook cells from the dataset metadata
        StatusResponse dataLoaderStatus = this.generateDataLoadingCellsAgent(datasetResponse);
        responseStatus = this.handleStatusResponse(dataLoaderStatus);

        if (responseStatus == null || responseStatus.getStatus().equals("failed")) {
            return responseStatus;
        }

        // generate the preprocessing cells
        StatusResponse preprocessorStatus = this.generatePreprocessingCellsAgent();
        responseStatus = this.handleStatusResponse(preprocessorStatus);

        if (responseStatus == null || responseStatus.getStatus().equals("failed")) {
            return responseStatus;
        }

        return responseStatus;
    }

    private StatusResponse generateDataLoadingCellsAgent(DatasetResponse datasetResponse) {
        log.info("GenerateDataLoadingCellsAgent: Generating Notebook");
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

            // update the agent memory
            updateAgentMemory(AgentState.GENERATING_DATA_LOADER_CELLS, prompt.getContents(), notebookContent);

            // convert the notebook content into a JSON object
            Map<String, Object> notebookContentMap = Util.getJsonFromContent(notebookContent);

            // wrap and return the notebook content payload
            Map<String, Object> payload;
            payload = Util.getWrappedJsonObject("notebook_content", notebookContentMap);

            // TODO: update hardcoded titanic with real notebook name
            StatusResponse creationStatus = performNotebookCreation("titanic", payload);
            return this.handleStatusResponse(creationStatus);

        } catch (Exception e) {
            log.error("Error reading data loading cells from LLM {}", e.getMessage());
        }
        return null;
    }

    private StatusResponse generatePreprocessingCellsAgent() {
        log.info("GeneratePreprocessingCellsAgent: Generating preprocessing cells");

        String memoryContext = Util.formatAgentMemories(agentMemories);

        String prompt = DATA_PREPROCESSING_PROMPT.render(Map.of(
                "memory", memoryContext
        ));

        try {
            // Call LLM
            String notebookContent = chatClient
                    .prompt(prompt)
                    .call()
                    .content();

            if (notebookContent == null || notebookContent.isBlank()) {
                log.error("Empty response from LLM during preprocessing cell generation");
                return StatusResponse.builder()
                        .status("Failure")
                        .message("Empty response from LLM")
                        .details("")
                        .build();
            }

            // Store to memory
            updateAgentMemory(AgentState.PREPROCESSING_DATA, prompt, notebookContent);

            // Wrap under correct contract
            List<Map<String, Object>> payload = Util.getJsonFromListContent(notebookContent);

            // Pass to workflow
            StatusResponse updateStatus = performNotebookUpdate("titanic", payload);
            return this.handleStatusResponse(updateStatus);

        } catch (Exception e) {
            log.error("Error reading preprocessing cells from LLM", e);

            return StatusResponse.builder()
                    .status("Failure")
                    .message("Errpr reading preprocessing cells from LLM")
                    .details(e.getMessage())
                    .build();
        }
    }

    private StatusResponse errorHandlerAgent(StatusResponse response) {
        log.info("ErrorHandlerAgent: Attempting to fix errors");

        String lastUserPrompt = agentMemories.get(agentMemories.toArray().length - 1).getUserInput();
        String lastAgentOutput = agentMemories.get(agentMemories.toArray().length - 1).getAgentOutput();

        // allow 3 attempts at error handling before quitting
        errorCounter += 1;
        if (errorCounter > 5) {
            return StatusResponse.builder()
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
            // retrieve the content from the LLM
            String content = chatClient
                    .prompt(prompt)
                    .call()
                    .content();

            if (agentMemories.get(agentMemories.toArray().length - 1).getAgentState() == AgentState.GENERATING_DATA_LOADER_CELLS) {
                // convert the notebook content into a JSON object
                Map<String, Object> notebookContentMap = Util.getJsonFromContent(content);

                // wrap and return the notebook content payload
                Map<String, Object> payload = Util.getWrappedJsonObject("notebook_content", notebookContentMap);
                log.info("ErrorHandlerAgent: Received notebook content from LLM: {}", content);
                StatusResponse creationStatus = performNotebookCreation("titanic", payload);
                return this.handleStatusResponse(creationStatus);
            } else {
                // convert the notebook content into a JSON object
                List<Map<String, Object>> payload = Util.getJsonFromListContent(content);

                // TODO: update hardcoded titanic with real notebook name
                log.info("ErrorHandlerAgent: Received cell contents from LLM: {}", content);
                StatusResponse updateStatus = performNotebookUpdate("titanic", payload);
                return this.handleStatusResponse(updateStatus);
            }
        } catch (Exception e) {
            log.error("Error reading content from LLM {}", e.getMessage());
        }
        return null;
    }

    private StatusResponse performNotebookCreation(String name, Map<String, Object> payload) {
        AgentState currentState = agentMemories.get(agentMemories.toArray().length - 1).getAgentState();
        log.info("Performing notebook creation on current state: {}", currentState.toString());
        if (Objects.requireNonNull(currentState) == AgentState.GENERATING_DATA_LOADER_CELLS) {
            log.info("Generating Data Loader Notebook Cells");
            return this.mlService.createNotebook(name, payload);
        }

        return StatusResponse.builder()
                .status("failed")
                .build();
    }

    private StatusResponse performNotebookUpdate(String name, List<Map<String, Object>> payload) {
        AgentState currentState = agentMemories.get(agentMemories.toArray().length - 1).getAgentState();
        log.info("Performing notebook update on current state: {}", currentState.toString());
        if (Objects.requireNonNull(currentState) == AgentState.PREPROCESSING_DATA) {
            log.info("Generating New Cells");
            return this.mlService.updateNotebook(name, payload);
        }
        return StatusResponse.builder()
                .status("failed")
                .build();
    }

    private void updateAgentMemory(AgentState state, String userInput, String agentOutput) {
        AgentMemory agentMemory = AgentMemory.builder()
                .agentState(state)
                .userInput(userInput)
                .agentOutput(agentOutput)
                .build();

        this.agentMemories.add(agentMemory);
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
