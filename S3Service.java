package com.example.springbootrestapi.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;

import org.springframework.stereotype.Service;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class S3Service {

    BasicAWSCredentials awsCreds = new BasicAWSCredentials("",
            "");
    //    @Bean
//    public AWSSimpleSystemsManagement awsSimpleSystemsManagement() {
//        return AWSSimpleSystemsManagementClientBuilder.standard()
//                .withCredentials(new AWSStaticCredentialsProvider(awsCreds)) // Set the credentials
//                .withRegion("us-east-1")  // Specify your region
//                .build();
//    }
    private final S3Client s3Client;

    public AmazonS3 amazonS3() {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds)) // Set the credentials
                .withRegion("us-east-1")  // Specify your region
                .build();
    }


    public String getTokenFromS3(String bucketName, String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        InputStream objectContent = s3Client.getObject(getObjectRequest);
        return new String(objectContent.readAllBytes(), StandardCharsets.UTF_8);
    }
}
