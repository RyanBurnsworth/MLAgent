package com.ryanburnsworth.mlagent.mlagent.services.agent;

import com.ryanburnsworth.mlagent.mlagent.models.AgentMemory;
import com.ryanburnsworth.mlagent.mlagent.models.DatasetMetadata;
import com.ryanburnsworth.mlagent.mlagent.models.ResponseStatus;
import com.ryanburnsworth.mlagent.mlagent.services.ml.MLService;
import com.ryanburnsworth.mlagent.mlagent.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.ryanburnsworth.mlagent.mlagent.util.Prompts.*;

@Service
public class AgentServiceImpl implements AgentService {
    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    private final ChatClient chatClient;
    private final MLService mlService;

    private List<AgentMemory> agentMemories;
    private int errorCounter = 0;
    private boolean isCreated = false;
    private String notebookName = "";

    AgentServiceImpl(ChatClient.Builder chatClientBuilder, MLService mlService) {
        this.chatClient = chatClientBuilder.build();
        this.mlService = mlService;
    }

    @Override
    public ResponseStatus machineLearningOrchestrator(String notebookName, String searchTerm) {
        resetAgentServiceState(notebookName);

        // Download dataset metadata
        DatasetMetadata datasetMetadata = mlService.fetchDatasetMetadata(searchTerm);

        // Execute workflow steps in order
        List<Supplier<ResponseStatus>> steps = List.of(
                () -> this.notebookCreatorAgent(datasetMetadata),
                () -> {
                    isCreated = true;
                    return this.notebookUpdaterAgent(DATA_PREPROCESSING_PROMPT);
                },
                () -> this.notebookUpdaterAgent(MODEL_TRAINING_PROMPT),
                () -> this.notebookUpdaterAgent(MODEL_EVALUATION_PROMPT)
        );

        return runStepsSequentially(steps);
    }

    private ResponseStatus runStepsSequentially(List<Supplier<ResponseStatus>> steps) {
        for (Supplier<ResponseStatus> step : steps) {
            ResponseStatus status = handleResponseStatus(step.get());
            if (status == null || "failed".equals(status.getStatus())) {
                return status;
            }
        }
        return ResponseStatus.builder().status("success").build();
    }

    private ResponseStatus notebookCreatorAgent(DatasetMetadata datasetMetadata) {
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
            Map<String, Object> payload = (Map<String, Object>) getPayloadFromLLM(prompt.getContents(), isCreated);

            ResponseStatus creationStatus = performNotebookAction(this.notebookName, payload);
            return this.handleResponseStatus(creationStatus);
        } catch (Exception e) {
            log.error("Error reading data loading cells from LLM {}", e.getMessage());
            return getResponseStatusError(e);
        }
    }

    private ResponseStatus notebookUpdaterAgent(PromptTemplate promptTemplate) {
        // log.info("NotebookUpdaterAgent: Generating preprocessing notebook cells");

        String memoryContext = Util.formatAgentMemories(agentMemories);
        String prompt = promptTemplate.render(Map.of(
                "memory", memoryContext
        ));

        try {
            ;
            List<Map<String, Object>> payload = (List<Map<String, Object>>) getPayloadFromLLM(prompt, isCreated);

            // Pass to workflow
            ResponseStatus updateStatus = performNotebookAction(this.notebookName, payload);
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
            Object payload = getPayloadFromLLM(prompt.getContents(), isCreated);

            ResponseStatus updateStatus = performNotebookAction(notebookName, payload);
            return this.handleResponseStatus(updateStatus);
        } catch (Exception e) {
            log.error("Error reading content from LLM {}", e.getMessage());
        }
        return null;
    }

    private Object getPayloadFromLLM(String prompt, Boolean isUpdatingNotebook) {
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

        updateAgentMemory(prompt, notebookContent);

        // if not updating, create the notebook
        if (!isUpdatingNotebook) {
            Map<String, Object> payload = Util.getJsonFromContent(notebookContent);

            // if payload is already wrapped, don't wrap twice.
            if (!payload.containsKey("notebook_content"))
                return Util.getWrappedJsonObject("notebook_content", payload);
            return payload;
        } else {
            // update the notebook
            return Util.getJsonFromListContent(notebookContent);
        }
    }

    private ResponseStatus performNotebookAction(String name, Object payload) {
        log.info("Performing notebook action");

        // Creating initial notebook
        if (!this.isCreated && payload instanceof Map) {
            log.info("Creating notebook with data loader cells");
            return this.mlService.createNotebook(name, (Map<String, Object>) payload);
        }

        // Updating notebook
        if (this.isCreated && payload instanceof List) {
            log.info("Updating notebook with preprocessing cells");
            return this.mlService.updateNotebook(name, (List<Map<String, Object>>) payload);
        }

        return getResponseStatusError(new Exception("Error performing notebook action"));
    }

    private void updateAgentMemory(String userInput, String agentOutput) {
        log.info("UpdateAgentMemory: Updating Agent Memory");
        AgentMemory agentMemory = AgentMemory.builder()
                .userInput(userInput)
                .agentOutput(agentOutput)
                .build();

        this.agentMemories.add(agentMemory);
    }

    private ResponseStatus handleResponseStatus(ResponseStatus status) {
        if ("success".equals(status.getStatus())) {
            log.info("Status Response is successful");
            return status;
        }

        // Response status is unsuccessful
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

    private void resetAgentServiceState(String notebookName) {
        // instantiate new memories for each ML orchestrator
        this.agentMemories = new ArrayList<>();

        // reset errorCounter
        this.errorCounter = 0;

        // reset isCreated flag
        this.isCreated = false;

        // update the notebookName
        this.notebookName = notebookName;
    }
}
