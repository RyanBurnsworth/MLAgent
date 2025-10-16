package com.ryanburnsworth.mlagent.mlagent.services.ml;

import com.ryanburnsworth.mlagent.mlagent.models.DatasetResponse;
import com.ryanburnsworth.mlagent.mlagent.models.StatusResponse;

import java.util.List;
import java.util.Map;

public interface MLService {
    DatasetResponse fetchDatasetMetadata(String searchTerm);

    StatusResponse createNotebook(String notebookName, Map<String, Object> notebookContent);

    StatusResponse updateNotebook(String notebookName, List<Map<String, Object>> notebookContent);
}
