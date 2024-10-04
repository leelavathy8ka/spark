package com.example.springbootrestapi.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.ssm.AWSSimpleSystemsManagement;
import com.amazonaws.services.ssm.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.ssm.model.GetParameterRequest;
import com.amazonaws.services.ssm.model.GetParameterResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DAGConfigRetriever {

    private static final Logger logger = Logger.getLogger(DAGConfigRetriever.class.getName());

    // AWS clients
    private final AmazonS3 s3Client;
    private final AWSSimpleSystemsManagement ssmClient;

    public DAGConfigRetriever() {
        this.s3Client = AmazonS3ClientBuilder.defaultClient();
        this.ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
    }

    public String retrieveConfiguration(String bucket, String key, String customer) throws Exception {
        String[] paths = key.split("/");
        
        if (paths.length < 6) {
            throw new IndexOutOfBoundsException("Invalid S3 key path structure.");
        }

        // Check customer exclusion
        String exclusions = System.getenv("CUSTOMER_EXCLUSIONS");
        if (exclusions != null) {
            List<String> exclusionList = List.of(exclusions.split(","));
            exclusionList.replaceAll(String::toUpperCase);
            exclusionList.replaceAll(String::trim);
            if (exclusionList.contains("ALL") || exclusionList.contains(customer.toUpperCase())) {
                return "EXCLUSION";
            }
        }

        // Retrieve the tenant secret path and the SSM parameter
        String tenantSecretPath = paths[2] + "/" + paths[4];
        String ssmParameterName = "/cobalt/dq/" + paths[2] + "/" + paths[4];

        // Retrieve parameter from SSM
        GetParameterRequest parameterRequest = new GetParameterRequest().withName(ssmParameterName);
        GetParameterResult parameterResult = ssmClient.getParameter(parameterRequest);

        String[] tenantValues = parameterResult.getParameter().getValue().split(":");
        String tenantBucket = tenantValues[0];
        String tenantKey = tenantValues[1];

        // Retrieve tenant details from S3
        S3Object s3Object = s3Client.getObject(tenantBucket, tenantKey);
        InputStream s3Data = s3Object.getObjectContent();
        Yaml yaml = new Yaml();
        Map<String, Object> tenantDetails = yaml.load(s3Data);

        // Retrieve configuration bucket and paths
        String configBucket = (String) ((Map<String, Object>) tenantDetails.get("dq_config")).get("bucket");
        List<Map<String, String>> pathList = (List<Map<String, String>>) ((Map<String, Object>) tenantDetails.get("dq_config")).get("dq_config_path");

        // Find the correct configuration key
        String configKey = pathList.stream()
                                   .filter(p -> p.containsKey(tenantSecretPath))
                                   .findFirst()
                                   .map(p -> p.get(tenantSecretPath))
                                   .orElseThrow(() -> new Exception("Configuration key not found for tenant."));

        // Dataset path
        String datasetPath = "s3://" + bucket + "/" + key;

        // Call helper method to get EMR creation details
        return getEMRCreationDetails(
                configBucket,
                configKey,
                (String) tenantDetails.get("tenant"),
                (String) tenantDetails.get("filetype"),
                customer,
                (String) tenantDetails.get("app_id"),
                (String) tenantDetails.get("cost_center"),
                (Map<String, Object>) tenantDetails.get("aws_config"),
                datasetPath
        );
    }

    private String getEMRCreationDetails(String configBucket, String configKey, String tenant, String filetype, String customer,
                                         String appId, String costCenter, Map<String, Object> awsConfig, String dataset) throws Exception {
        // Build the configuration as a JSON string using ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> config = Map.of(
                "conf", Map.of(
                        "bucket", configBucket,
                        "key", configKey,
                        "dataset", dataset,
                        "tenant", tenant,
                        "filetype", filetype,
                        "customer", customer,
                        "app_id", appId,
                        "cost_center", costCenter,
                        "aws_config", awsConfig,
                        "partitions", Map.of()
                )
        );

        return objectMapper.writeValueAsString(config);
    }
}
