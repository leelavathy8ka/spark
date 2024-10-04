package com.example.springbootrestapi.controller;

import com.example.springbootrestapi.service.Emr;

import java.util.Map;

public class Implementation {

    // Replace this with your EMR class that handles configuration retrieval.
    private final EMR emr = new EMR();

    public void processEvent(Map<String, Object> message) {
        String data = null;

        // Check if the 'detail' key exists
        if (message.containsKey("detail")) {
            Map<String, Object> detail = (Map<String, Object>) message.get("detail");

            // Check if 'bucket' and 'object' keys exist
            if (detail.containsKey("bucket") && detail.containsKey("object")) {
                Map<String, String> bucket = (Map<String, String>) detail.get("bucket");
                Map<String, String> object = (Map<String, String>) detail.get("object");

                String tenantBucket = bucket.get("name");
                String tenantKey = object.get("key");

                // Call EMR method to retrieve configuration
                data = Emr.retrieveConfiguration(tenantBucket, tenantKey);
            }
        }

        if (data != null) {
            // Handle exclusion case
            if (data.equals("EXCLUSION")) {
                return;
            }

            // Convert data to Map (equivalent to eval in Python)
            Map<String, Object> dataDict = parseDataToMap(data);

            // Check if bucket and key exist in the configuration
            if (dataDict != null && dataDict.containsKey("conf")) {
                Map<String, Object> conf = (Map<String, Object>) dataDict.get("conf");
                if (conf.containsKey("bucket") && conf.containsKey("key")) {
                    // Trigger Airflow DAG
                    String dag = System.getenv("AIRFLOW_DAG");
                    String airflowUrl = System.getenv("AIRFLOW_URL");

                    DAG targetDag = new DAG();
                    targetDag.triggerDAG(dag, data, airflowUrl);
                }
            }
        }
    }

    // Method to parse data (in JSON format) into a Map
    private Map<String, Object> parseDataToMap(String data) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(data, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
