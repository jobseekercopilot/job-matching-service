package com.jobseekercopilot.jobmatching.client;

import com.jobseekercopilot.jobmatching.dto.ApplicationRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class ApplicationTrackerClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ApplicationTrackerClient(
            RestTemplate restTemplate,
            @Value("${services.application-tracker-service.base-url:http://localhost:8088}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<ApplicationRecord> getApplicationsForUser(String userId) {
        try {
            var response = restTemplate.exchange(
                    baseUrl + "/api/v1/applications/user/{userId}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ApplicationRecord>>() {
                    },
                    userId);
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (RestClientResponseException ex) {
            throw new ApplicationTrackerUnavailableException("Application tracker request failed", ex);
        }
    }

    public static class ApplicationTrackerUnavailableException extends RuntimeException {
        public ApplicationTrackerUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
