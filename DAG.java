package com.example.springbootrestapi.controller;

import java.util.Base64;
import java.util.Map;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.net.URI;
import java.io.IOException;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class DAG {
    private static final Logger logger = Logger.getLogger(DAG.class.getName());

    public void triggerDAG(String dag, String data, String airflowUrl, String secretId, String region) {
        try {
            // Retrieve the username and password from AWS Secrets Manager
            String[] credentials = retrieveSecret(secretId, "airflow_url", region);
            String user = credentials[0];
            String pwd = credentials[1];

            // Create the Airflow API URL
            String postUrl = airflowUrl + "/api/v1/dags/" + dag + "/dagRuns";

            // Create the request with headers and body
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(postUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .build();

            // Send the request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check response status
            if (response.statusCode() == 200) {
                logger.info("DAG " + dag + " triggered successfully.");
            } else {
                logger.severe("Failed to trigger DAG " + dag + ". Status Code: " + response.statusCode()
                        + ", Response: " + response.body());
            }

        } catch (IOException | InterruptedException e) {
            logger.severe("Error occurred while triggering the DAG: " + e.getMessage());
        }
    }

    public static String[] retrieveSecret(String secretId, String key, String region) {
        AWSSecretsManager secretsManager = AWSSecretsManagerClientBuilder.standard()
                .withRegion(region)
                .build();

        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretId);
        GetSecretValueResult getSecretValueResult = secretsManager.getSecretValue(getSecretValueRequest);

        String secretString = getSecretValueResult.getSecretString();

        try {
            // Parse the secret string as a JSON object
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> secretMap = objectMapper.readValue(secretString, Map.class);

            // Decode the Base64-encoded secret
            byte[] decodedBytes = Base64.getDecoder().decode(secretMap.get(key));
            String decodedSecret = new String(decodedBytes, StandardCharsets.UTF_8);

            // Split the decoded secret into username and password
            return decodedSecret.split(":");
        } catch (JsonProcessingException e) {
            logger.severe("Failed to process secret: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
