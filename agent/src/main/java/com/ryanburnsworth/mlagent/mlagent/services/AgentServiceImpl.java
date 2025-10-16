package com.ryanburnsworth.mlagent.mlagent.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.ryanburnsworth.mlagent.mlagent.util.Prompts.DATA_LOADING_PROMPT;

@Service
public class AgentServiceImpl implements AgentService {
    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    private final ChatClient chatClient;

    AgentServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> dataLoaderAgent(String title, String subtitle, String description, String datasets) {
        Prompt prompt = DATA_LOADING_PROMPT.create(
                Map.of(
                        "title", title,
                        "subtitle", subtitle,
                        "description", description,
                        "datasets", datasets
                )
        );

        try {
            String notebookContent = chatClient
                    .prompt(prompt)
                    .call()
                    .content();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> notebookContentMap = mapper.readValue(notebookContent, Map.class);

            Map<String, Object> payload = new HashMap<>();
            payload.put("notebook_content", notebookContentMap);

            return payload;
        } catch (Exception e) {
            log.error("Error reading notebook content from LLM {}", e.getMessage());
            return null;
        }
    }

    public void EvaluatorAgent() {

    }
}
