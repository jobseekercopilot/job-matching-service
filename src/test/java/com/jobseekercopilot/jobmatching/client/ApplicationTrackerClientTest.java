package com.jobseekercopilot.jobmatching.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.jobseekercopilot.generated.applicationtracker.api.ApplicationRecordsApi;
import com.jobseekercopilot.generated.applicationtracker.client.ApiClient;
import com.jobseekercopilot.generated.applicationtracker.client.auth.ApiKeyAuth;
import com.jobseekercopilot.jobmatching.dto.ApplicationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class ApplicationTrackerClientTest {

    private static final String BASE_URL = "https://application-tracker.test";
    private static final String READER_TOKEN =
            "test-only-job-matching-reader-token-32-bytes";

    @Test
    void sendsExactlyOneReaderCredentialAndMapsTheGeneratedResponse() {
        TestClient testClient = client(READER_TOKEN);
        testClient.server.expect(requestTo(
                        BASE_URL + "/api/v1/applications/user/user-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(
                                request.getHeaders().get("X-Service-Token"))
                        .containsExactly(READER_TOKEN))
                .andExpect(request -> assertThat(request.getHeaders())
                        .doesNotContainKey(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess("""
                        [{
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "userId": "user-1",
                          "jobId": "job-1",
                          "canonicalJobId": "canonical-1",
                          "provider": "REED",
                          "externalJobId": "external-1",
                          "provenance": "GENERATED",
                          "jobTitle": "Developer",
                          "companyName": "Example Ltd",
                          "location": "London",
                          "cvDocumentId": "cv-1",
                          "coverLetterDocumentId": "cl-1",
                          "status": "APPLIED",
                          "createdAt": "2026-07-24T09:00:00",
                          "updatedAt": "2026-07-24T10:00:00",
                          "appliedAt": "2026-07-24T09:30:00",
                          "version": 2
                        }]
                        """, MediaType.APPLICATION_JSON));

        var applications = testClient.client.getApplicationsForUser("user-1");

        assertThat(applications).hasSize(1);
        var application = applications.get(0);
        assertThat(application.getUserId()).isEqualTo("user-1");
        assertThat(application.getCanonicalJobId()).isEqualTo("canonical-1");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(application.getUpdatedAt())
                .isEqualTo(LocalDateTime.of(2026, 7, 24, 10, 0));
        testClient.server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404})
    void convertsAuthenticationAuthorizationAndOwnerFailuresToStableUnavailable(
            int statusCode) {
        String rejectedToken = statusCode == 401
                ? "test-only-wrong-reader-token-value-32-bytes"
                : READER_TOKEN;
        TestClient testClient = client(rejectedToken);
        testClient.server.expect(requestTo(
                        BASE_URL + "/api/v1/applications/user/foreign-owner"))
                .andExpect(request -> assertThat(
                                request.getHeaders().get("X-Service-Token"))
                        .containsExactly(rejectedToken))
                .andRespond(withStatus(HttpStatus.valueOf(statusCode))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(statusCode == 404
                                ? """
                                  {"status":404,"message":"Application record not found.",
                                   "timestamp":"2026-07-24T10:00:00"}
                                  """
                                : statusCode == 401
                                ? """
                                  {"code":"AUTHENTICATION_REQUIRED",
                                   "message":"Valid authentication is required."}
                                  """
                                : """
                                  {"code":"ACCESS_DENIED","message":"Access is denied."}
                                  """));

        assertThatThrownBy(() ->
                testClient.client.getApplicationsForUser("foreign-owner"))
                .isInstanceOf(
                        ApplicationTrackerClient.ApplicationTrackerUnavailableException.class)
                .hasMessage("Application tracker request failed")
                .hasMessageNotContaining(rejectedToken);
        testClient.server.verify();
    }

    private TestClient client(String readerToken) {
        RestTemplate restTemplate = new RestTemplate();
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(BASE_URL);
        ApiKeyAuth serviceToken =
                (ApiKeyAuth) apiClient.getAuthentication("serviceToken");
        serviceToken.setApiKey(readerToken);
        ApplicationTrackerClient client =
                new ApplicationTrackerClient(new ApplicationRecordsApi(apiClient));
        return new TestClient(client, MockRestServiceServer.createServer(restTemplate));
    }

    private record TestClient(
            ApplicationTrackerClient client,
            MockRestServiceServer server) {
    }
}
