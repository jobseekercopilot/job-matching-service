package com.jobseekercopilot.jobmatching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jobseekercopilot.jobmatching.client.ApplicationTrackerClient;
import com.jobseekercopilot.jobmatching.dto.ApplicationRecord;
import com.jobseekercopilot.jobmatching.dto.ApplicationStatus;
import com.jobseekercopilot.jobmatching.dto.JobMatchJob;
import com.jobseekercopilot.jobmatching.dto.JobSourceReference;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobMatchingServiceTest {

    private final FakeApplicationTrackerClient applicationTrackerClient = new FakeApplicationTrackerClient();
    private final JobMatchingService service = new JobMatchingService(applicationTrackerClient);

    @Test
    void enrichesByProviderAndExternalJobId() {
        UUID applicationId = UUID.randomUUID();
        ApplicationRecord record = application(applicationId);
        applicationTrackerClient.records = List.of(record);

        JobMatchJob job = job("123456", "Software Developer", "Matchtech", "Dorking");
        var response = service.enrichJobs("user-1", List.of(job));

        JobMatchJob enriched = response.getJobs().get(0);
        assertThat(enriched.getApplicationStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(enriched.getApplicationId()).isEqualTo(applicationId);
        assertThat(enriched.getCvDocumentId()).isEqualTo("cv-1");
        assertThat(enriched.getCoverLetterDocumentId()).isEqualTo("cl-1");
    }

    @Test
    void fallsBackToNormalisedFields() {
        ApplicationRecord record = application(UUID.randomUUID());
        record.setExternalJobId(null);
        record.setJobId(null);
        applicationTrackerClient.records = List.of(record);

        JobMatchJob job = job(null, " software, developer ", "MATCHTECH", "Dorking");
        var response = service.enrichJobs("user-1", List.of(job));

        assertThat(response.getJobs().get(0).getApplicationStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void marksUnmatchedJobsNew() {
        applicationTrackerClient.records = List.of();

        var response = service.enrichJobs("user-1", List.of(job("999", "Tester", "Example", "London")));

        assertThat(response.getJobs().get(0).getApplicationStatus()).isEqualTo(ApplicationStatus.NEW);
        assertThat(response.getJobs().get(0).getApplicationId()).isNull();
    }

    @Test
    void enrichesSavedApplicationWithoutInventingDocumentState() {
        ApplicationRecord record = application(UUID.randomUUID());
        record.setStatus(ApplicationStatus.SAVED);
        record.setCvDocumentId(null);
        record.setCoverLetterDocumentId(null);
        record.setAppliedAt(null);
        applicationTrackerClient.records = List.of(record);

        JobMatchJob enriched = service.enrichJobs(
                "user-1",
                List.of(job("123456", "Software Developer", "Matchtech", "Dorking")))
                .getJobs()
                .get(0);

        assertThat(enriched.getApplicationStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(enriched.getApplicationId()).isEqualTo(record.getId());
        assertThat(enriched.getCvDocumentId()).isNull();
        assertThat(enriched.getCoverLetterDocumentId()).isNull();
        assertThat(enriched.getAppliedAt()).isNull();
        assertThat(enriched.getApplicationUpdatedAt())
                .isEqualTo(OffsetDateTime.parse(
                        "2026-06-30T10:00:00Z"));
    }

    @Test
    void serializesRetainedApplicationTimestampsWithUtcOffsets()
            throws Exception {
        ApplicationRecord record = application(UUID.randomUUID());
        applicationTrackerClient.records = List.of(record);

        var response = service.enrichJobs(
                "user-1",
                List.of(job(
                        "123456",
                        "Software Developer",
                        "Matchtech",
                        "Dorking")));

        var json = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .valueToTree(response);
        assertThat(json.at("/jobs/0/appliedAt").asText())
                .isEqualTo("2026-06-30T10:00:00Z");
        assertThat(json.at("/jobs/0/applicationUpdatedAt").asText())
                .isEqualTo("2026-06-30T10:00:00Z");
    }

    @Test
    void preservesDistanceWhenEnrichingJobs() {
        applicationTrackerClient.records = List.of();
        JobMatchJob job = job("999", "Tester", "Example", "London");
        job.setDistanceMiles(12.345);

        var response = service.enrichJobs("user-1", List.of(job));

        assertThat(response.getJobs().get(0).getDistanceMiles()).isEqualTo(12.345);
    }

    @Test
    void preservesProviderSourceTimestampOffsetWhenEnrichingJobs()
            throws Exception {
        applicationTrackerClient.records = List.of();
        JobMatchJob job = job("999", "Tester", "Example", "London");
        JobSourceReference source = new JobSourceReference();
        source.setProvider("REED");
        source.setProviderPostedAt(OffsetDateTime.parse("2026-08-06T09:30:00Z"));
        job.setSources(List.of(source));

        var response = service.enrichJobs("user-1", List.of(job));

        var json = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .valueToTree(response);
        assertThat(json.at("/jobs/0/sources/0/providerPostedAt").asText())
                .isEqualTo("2026-08-06T09:30:00Z");
    }

    private ApplicationRecord application(UUID applicationId) {
        ApplicationRecord record = new ApplicationRecord();
        record.setId(applicationId);
        record.setProvider("REED");
        record.setExternalJobId("123456");
        record.setJobTitle("Software Developer");
        record.setCompanyName("Matchtech");
        record.setLocation("Dorking");
        record.setCvDocumentId("cv-1");
        record.setCoverLetterDocumentId("cl-1");
        record.setStatus(ApplicationStatus.APPLIED);
        record.setAppliedAt(LocalDateTime.of(2026, 6, 30, 10, 0));
        record.setUpdatedAt(LocalDateTime.of(2026, 6, 30, 10, 0));
        return record;
    }

    private JobMatchJob job(String id, String title, String company, String location) {
        JobMatchJob job = new JobMatchJob();
        job.setId(id);
        job.setTitle(title);
        job.setCompany(company);
        job.setLocation(location);
        return job;
    }

    private static class FakeApplicationTrackerClient extends ApplicationTrackerClient {
        private List<ApplicationRecord> records = List.of();

        FakeApplicationTrackerClient() {
            super(null);
        }

        @Override
        public List<ApplicationRecord> getApplicationsForUser(String userId) {
            return records;
        }
    }
}
