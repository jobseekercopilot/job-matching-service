package com.jobseekercopilot.jobmatching.client;

import com.jobseekercopilot.generated.applicationtracker.api.ApplicationRecordsApi;
import com.jobseekercopilot.generated.applicationtracker.model.ApplicationRecordResponse;
import com.jobseekercopilot.jobmatching.dto.ApplicationRecord;
import com.jobseekercopilot.jobmatching.dto.ApplicationStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class ApplicationTrackerClient {

    private final ApplicationRecordsApi applicationRecordsApi;

    public ApplicationTrackerClient(ApplicationRecordsApi applicationRecordsApi) {
        this.applicationRecordsApi = applicationRecordsApi;
    }

    public List<ApplicationRecord> getApplicationsForUser(String userId) {
        try {
            List<ApplicationRecordResponse> response =
                    applicationRecordsApi.getApplicationsForUser(userId);
            return response == null
                    ? List.of()
                    : response.stream().map(this::toApplicationRecord).toList();
        } catch (RestClientResponseException ex) {
            throw new ApplicationTrackerUnavailableException("Application tracker request failed", ex);
        } catch (RestClientException ex) {
            throw new ApplicationTrackerUnavailableException("Application tracker transport failed", ex);
        }
    }

    private ApplicationRecord toApplicationRecord(ApplicationRecordResponse source) {
        ApplicationRecord record = new ApplicationRecord();
        record.setId(source.getId());
        record.setUserId(source.getUserId());
        record.setJobId(source.getJobId());
        record.setCanonicalJobId(source.getCanonicalJobId());
        record.setProvider(source.getProvider());
        record.setExternalJobId(source.getExternalJobId());
        record.setJobTitle(source.getJobTitle());
        record.setCompanyName(source.getCompanyName());
        record.setLocation(source.getLocation());
        record.setCvDocumentId(source.getCvDocumentId());
        record.setCoverLetterDocumentId(source.getCoverLetterDocumentId());
        record.setStatus(source.getStatus() == null
                ? null
                : ApplicationStatus.valueOf(source.getStatus().getValue()));
        record.setCreatedAt(source.getCreatedAt());
        record.setUpdatedAt(source.getUpdatedAt());
        record.setAppliedAt(source.getAppliedAt());
        return record;
    }

    public static class ApplicationTrackerUnavailableException extends RuntimeException {
        public ApplicationTrackerUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
