package com.jobseekercopilot.jobmatching.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jobseekercopilot.generated.applicationtracker.client.ApiClient;
import com.jobseekercopilot.generated.applicationtracker.client.auth.ApiKeyAuth;
import org.junit.jupiter.api.Test;

class ApplicationTrackerApiConfigTest {

    private static final String READER_TOKEN =
            "test-only-job-matching-reader-token-32-bytes";

    private final ApplicationTrackerApiConfig config =
            new ApplicationTrackerApiConfig();

    @Test
    void rejectsMissingAndShortReaderCredentials() {
        assertThatThrownBy(() ->
                config.applicationTrackerApiClient("http://application-tracker", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
        assertThatThrownBy(() ->
                config.applicationTrackerApiClient("http://application-tracker", "too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void configuresOnlyTheGeneratedServiceTokenAuthentication() {
        ApiClient apiClient = config.applicationTrackerApiClient(
                "http://application-tracker",
                READER_TOKEN);

        ApiKeyAuth serviceToken = (ApiKeyAuth)
                apiClient.getAuthentication(ApplicationTrackerApiConfig.SERVICE_TOKEN_AUTH);
        assertThat(serviceToken.getApiKey()).isEqualTo(READER_TOKEN);
        assertThat(apiClient.getAuthentication("bearerAuth")).isNotNull();
    }
}
