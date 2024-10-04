package com.example.springbootrestapi.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.ssm.AmazonSSM;
import com.amazonaws.services.ssm.AmazonSSMClientBuilder;
import com.amazonaws.services.ssm.model.GetParameterRequest;
import com.amazonaws.services.ssm.model.GetParameterResult;
import com.amazonaws.services.s3.model.S3Object;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Emr {

    public static String retrieveConfiguration(String bucket, String key, String customer) {
        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        String[] paths = key.split("/");

        if (paths.length < 6) {
            throw new IndexOutOfBoundsException("Key does not have enough segments");
        }

        String exclusions = System.getenv("CUSTOMER_EXCLUSIONS");
        if (exclusions != null) {
            String[] exclusionList = exclusions.split(",");
            for (String exclusion : exclusionList) {
                if ("ALL".equalsIgnoreCase(exclusion.trim()) || exclusion.trim().equalsIgnoreCase(customer)) {
                    return "EXCLUSION";
                }
            }
        }

        String tenantSecretPath = paths[2] + "/" + paths[4];
        String ssmParameterName = "/cobalt/dq/" + paths[2] + "/" + paths[4];
        AmazonSSM ssmClient = AmazonSSMClientBuilder.defaultClient();

        GetParameterRequest parameterRequest = new GetParameterRequest()
                .withName(ssmParameterName)
                .withWithDecryption(true);
        GetParameterResult parameterResult = ssmClient.getParameter(parameterRequest);
        String[] tenantDetails = parameterResult.getParameter().getValue().split(":");
        String tenantBucket = tenantDetails[0];
        String tenantKey = tenantDetails[1];

        S3Object s3Object = s3Client.getObject(tenantBucket, tenantKey);
        Yaml yaml = new Yaml();
        Map<String, Object> tenantData = yaml.load(s3Object.getObjectContent());

        Map<String, Object> dqConfig = (Map<String, Object>) tenantData.get("dq_config");
        String configBucket = (String) dqConfig.get("bucket");
        List<Map<String, Object>> pathList = (List<Map<String, Object>>) dqConfig.get("dq_config_path");

        Optional<Map<String, Object>> configEntry = pathList.stream()
                .filter(d -> d.containsKey(tenantSecretPath))
                .findFirst();
        String configKey = configEntry.map(d -> (String) d.get(tenantSecretPath)).orElse(null);
        String datasetPath = "s3://" + bucket + "/" + key;

        String cfg = getEmrCreationDetails(configBucket, configKey, 
            (String) tenantData.get("tenant"), 
            (String) tenantData.get("filetype"), 
            customer, 
            (String) tenantData.get("app_id"), 
            (String) tenantData.get("cost_center"), 
            (Map<String, Object>) tenantData.get("aws_config"), 
            datasetPath
        );

        return cfg;
    }

    private static String getEmrCreationDetails(
            String configBucket,
            String configKey,
            String tenant,
            String filetype,
            String customer,
            String appId,
            String costCenter,
            Map<String, Object> awsConfig,
            String dataset) {
        Map<String, Object> conf = new HashMap<>();
        conf.put("bucket", configBucket);
        conf.put("key", configKey);
        conf.put("dataset", dataset);
        conf.put("tenant", tenant);
        conf.put("filetype", filetype);
        conf.put("customer", customer);
        conf.put("app_id", appId);
        conf.put("cost_center", costCenter);
        conf.put("aws_config", awsConfig);
        conf.put("partitions", new HashMap<>());

        return new Yaml().dump(conf);
    }
}
