package com.ryanburnsworth.mlagent.mlagent.services.ml;

import com.ryanburnsworth.mlagent.mlagent.models.DatasetResponse;
import com.ryanburnsworth.mlagent.mlagent.models.StatusResponse;

import java.util.Map;

public interface MLService {
    DatasetResponse fetchDatasetMetadata(String searchTerm);

    StatusResponse generateDataloaderNotebookCells(String notebookName, Map<String, Object> notebookContent);
}
