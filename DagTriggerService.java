package com.example.springbootrestapi.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@Service
public class DagTriggerService {

    @Autowired
    private AwsSnsService snsService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AppProperties appProperties;

    public void triggerDag(Map<String, String> snsMsg, Map<String, String> ssmParams) {
        String token = retrieveSecret();
        String executePipelineApiUrl = String.format("%s/executePipeline?pipelineName=%s&projectId=%s",
                appProperties.getSparkflowsUrl(), snsMsg.get("pipelineName"), snsMsg.get("projectId"));

        String workflowParameters = convertJsonToVariables(snsMsg, ssmParams);
        System.out.println("Workflow Parameters: " + workflowParameters);

        try {
            URI uri = new URI(executePipelineApiUrl);
            Map<String, Object> requestBody = Map.of(
                    "userName", "admin",
                    "workflowParameters", workflowParameters
            );

            // Create headers and set the token
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Accept", "application/json");
            headers.set("token", token);

            // Send the POST request
            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(uri, entity, String.class);

            if (response.getStatusCodeValue() == 200) {
                JSONObject jsonResponse = new JSONObject(response.getBody());
                String executionId = jsonResponse.getString("workflowExecutionId");
                System.out.println("Execution result URL: " + appProperties.getSparkflowsUrl() + "/#/view-workflow-result/" + executionId);
            } else {
                sendFailureNotification(snsMsg);
            }
        } catch (Exception e) {
            System.err.println("Error while executing DAG: " + e.getMessage());
            sendFailureNotification(snsMsg);
        }
    }

    // Mock method to simulate retrieving the secret
    public String retrieveSecret() {
        return "your_token_here"; // Replace with actual token retrieval logic
    }

    // Mock method to simulate converting JSON to variables
    public String convertJsonToVariables(Map<String, String> snsMsg, Map<String, String> ssmParams) {
        StringBuilder output = new StringBuilder();
        String[] paramList = ssmParams.get("parameterkeys").split("\\|"); // Splitting by "|"

        for (String key : paramList) {
            if (snsMsg.containsKey(key)) {
                output.append("--var ").append(key).append("=").append(snsMsg.get(key)).append(" ");
            } else if (ssmParams.containsKey(key)) {
                output.append("--var ").append(key).append("=").append(ssmParams.get(key)).append(" ");
            }
        }

        return output.toString();
    }

    private void sendFailureNotification(Map<String, String> snsMsg) {
        String pipelineName = snsMsg.get("pipelineName");
        String batch = snsMsg.get("batch");

        // Create the subject and message
        String subject = String.format("Spark Flows EDO Failure: Failed to trigger Pipeline-%s for batch -%s", pipelineName, batch);
        JSONObject messageJson = new JSONObject();
        messageJson.put("subject", subject);
        messageJson.put("message", snsMsg);

        snsService.publishMessage(subject, messageJson.toString());
    }
}
