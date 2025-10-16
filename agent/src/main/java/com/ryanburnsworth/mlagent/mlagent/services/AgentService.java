package com.ryanburnsworth.mlagent.mlagent.services;

import java.util.Map;

public interface AgentService {

    public Map<String, Object> dataLoaderAgent(String title, String subtitle, String description, String datasets);

}
