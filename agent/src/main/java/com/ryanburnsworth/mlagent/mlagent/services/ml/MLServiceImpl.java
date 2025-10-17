package com.ryanburnsworth.mlagent.mlagent.services.ml;

import com.ryanburnsworth.mlagent.mlagent.models.DatasetMetadata;
import com.ryanburnsworth.mlagent.mlagent.models.ResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class MLServiceImpl implements MLService {
    private static final Logger log = LoggerFactory.getLogger(MLServiceImpl.class);
    private final WebClient webClient;

    public MLServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public DatasetMetadata fetchDatasetMetadata(String searchTerm) {
        log.info("fetchDatasetMetadata: Fetching Dataset Metadata using search term {}: ", searchTerm);
        try {
            return webClient.get()
                    .uri("/dataset/download/" + searchTerm)
                    .retrieve()
                    .bodyToMono(DatasetMetadata.class)
                    .block();
        } catch (Exception e) {
            // TODO: Add error handling
            log.error("fetchDatasetMetadata: Error fetching dataset metadata: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public ResponseStatus createNotebook(String notebookName, Map<String, Object> notebookContent) {
        try {
            log.info("createNotebook: Creating notebook on ML Service");
            return webClient.post()
                    .uri("/notebook/create/" + notebookName)
                    .body(BodyInserters.fromValue(notebookContent))
                    .exchangeToMono(clientResponse ->
                            clientResponse.bodyToMono(ResponseStatus.class))
                    .block();
        } catch (Exception e) {
            // TODO: Add error handling
            log.error("createNotebook: Error creating notebook on ML Service: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public ResponseStatus updateNotebook(String notebookName, List<Map<String, Object>> notebookContent) {
        try {
            log.info("updateNotebook: Updating notebook on ML Service");
            return webClient.post()
                    .uri("/notebook/update/" + notebookName)
                    .body(BodyInserters.fromValue(notebookContent))
                    .exchangeToMono(clientResponse ->
                            clientResponse.bodyToMono(ResponseStatus.class))
                    .block();
        } catch (Exception e) {
            // TODO: Add error handling
            log.error("updateNotebook: Error updating notebook on ML Service: {}", e.getMessage());
            return null;
        }
    }
}
