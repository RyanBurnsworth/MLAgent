package com.ryanburnsworth.mlagent.mlagent.services.ml;

import com.ryanburnsworth.mlagent.mlagent.models.DatasetResponse;
import com.ryanburnsworth.mlagent.mlagent.models.StatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class MLServiceImpl implements MLService {
    private final WebClient webClient;

    public MLServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public DatasetResponse fetchDatasetMetadata(String searchTerm) {
        return webClient.get()
                .uri("/dataset/download/" + searchTerm)
                .retrieve()
                .bodyToMono(DatasetResponse.class)
                .block();
    }

    @Override
    public StatusResponse createNotebook(String notebookName, Map<String, Object> notebookContent) {
        try {
            return webClient.post()
                    .uri("/notebook/create/" + notebookName)
                    .body(BodyInserters.fromValue(notebookContent))
                    .exchangeToMono(clientResponse ->
                            clientResponse.bodyToMono(StatusResponse.class))
                    .block();
        } catch (Exception e) {
            // TODO: Add error handling
            System.out.println(e.getMessage());
            return null;
        }
    }

    @Override
    public StatusResponse updateNotebook(String notebookName, List<Map<String, Object>> notebookContent) {
        try {
            return webClient.post()
                    .uri("/notebook/update/" + notebookName)
                    .body(BodyInserters.fromValue(notebookContent))
                    .exchangeToMono(clientResponse ->
                            clientResponse.bodyToMono(StatusResponse.class))
                    .block();
        } catch (Exception e) {
            // TODO: Add error handling
            System.out.println(e.getMessage());
            return null;
        }
    }
}
