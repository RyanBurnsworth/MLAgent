package com.ryanburnsworth.mlagent.mlagent.services.ml;

import com.ryanburnsworth.mlagent.mlagent.models.DatasetMetadata;
import com.ryanburnsworth.mlagent.mlagent.models.ResponseStatus;

import java.util.List;
import java.util.Map;

public interface MLService {
    DatasetMetadata fetchDatasetMetadata(String searchTerm);

    ResponseStatus createNotebook(String notebookName, Map<String, Object> notebookContent);

    ResponseStatus updateNotebook(String notebookName, List<Map<String, Object>> notebookContent);
}
