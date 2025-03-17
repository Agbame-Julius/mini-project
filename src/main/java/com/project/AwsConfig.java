package com.project;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.amazonaws.regions.Regions.US_EAST_1;

@Configuration
public class AwsConfig {
    @Bean
    public AmazonS3 amazonS3() {

        return AmazonS3Client.builder()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(US_EAST_1)
                .build();
    }


}
