package com.jobseekercopilot.jobmatching.config;

import com.jobseekercopilot.generated.applicationtracker.api.ApplicationRecordsApi;
import com.jobseekercopilot.generated.applicationtracker.client.ApiClient;
import com.jobseekercopilot.generated.applicationtracker.client.auth.ApiKeyAuth;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationTrackerApiConfig {

    static final String SERVICE_TOKEN_AUTH = "serviceToken";
    static final int MINIMUM_TOKEN_BYTES = 32;

    @Bean
    ApiClient applicationTrackerApiClient(
            @Value("${services.application-tracker-service.base-url:http://localhost:8088}")
                    String basePath,
            @Value("${services.application-tracker-service.reader-token:}")
                    String readerToken) {
        if (readerToken == null
                || readerToken.getBytes(StandardCharsets.UTF_8).length < MINIMUM_TOKEN_BYTES) {
            throw new IllegalStateException(
                    "Application Tracker reader credential must contain at least 32 bytes");
        }

        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(basePath);
        ApiKeyAuth serviceToken =
                (ApiKeyAuth) apiClient.getAuthentication(SERVICE_TOKEN_AUTH);
        serviceToken.setApiKey(readerToken);
        return apiClient;
    }

    @Bean
    ApplicationRecordsApi applicationRecordsApi(ApiClient applicationTrackerApiClient) {
        return new ApplicationRecordsApi(applicationTrackerApiClient);
    }
}
