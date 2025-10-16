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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ryanburnsworth.mlagent.mlagent.util.Prompts.DATA_LOADING_PROMPT;

@Service
public class AgentServiceImpl implements AgentService {
    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    private final ChatClient chatClient;
    private final MLService mlService;
    private final List<AgentMemory> agentMemories;

    AgentServiceImpl(ChatClient.Builder chatClientBuilder, MLService mlService) {
        this.chatClient = chatClientBuilder.build();
        this.mlService = mlService;
        this.agentMemories = new ArrayList<>();
    }

    @Override
    public String performAgenticMachineLearning(String searchTerm) {
        // download the hottest dataset from Kaggle and it's metadata using a search term
        DatasetResponse datasetResponse = mlService.fetchDatasetMetadata(searchTerm);

        // generate the dataset loading notebook cells from the dataset metadata
        StatusResponse status = this.generateDataLoadingCellsAgent(datasetResponse);

        if (Objects.equals(status.getStatus(), "success")) {
            // TODO: Move on to the next step
            return "success";
        } else {
            // TODO: Evaluate and fix the error if possible
            return "Error: " + status.getMessage() + " " + status.getDetails();
        }
    }

    public StatusResponse generateDataLoadingCellsAgent(DatasetResponse datasetResponse) {
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

            // convert the notebook content into a JSON object
            Map<String, Object> notebookContentMap = Util.getJsonFromContent(notebookContent);

            // wrap the notebook content payload
            Map<String, Object> payload = Util.getWrappedJsonObject("notebook_content", notebookContentMap);

            // update the agent memory
            AgentMemory agentMemory = AgentMemory.builder()
                    .agentState(AgentState.DOWNLOADING_DATASET)
                    .userInput(prompt.getContents())
                    .agentOutput(notebookContent)
                    .build();

            agentMemories.add(agentMemory);

            return this.mlService.generateDataloaderNotebookCells("titanic", payload);
        } catch (Exception e) {
            log.error("Error reading notebook content from LLM {}", e.getMessage());
            return null;
        }
    }
}
