package com.ryanburnsworth.mlagent.mlagent.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DatasetResponse {

    @JsonProperty("dataset_name")
    private String datasetName;

    @JsonProperty("title")
    private String title;

    @JsonProperty("subtitle")
    private String subtitle;

    @JsonProperty("description")
    private String description;

    @JsonProperty("datasets")
    private List<String> datasets;

    public String getDatasets() {
        return String.join(",", datasets);
    }
}
