package com.jobseekercopilot.jobmatching.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ApplicationRecord {
    private UUID id;
    private String userId;
    private String jobId;
    private String canonicalJobId;
    private String provider;
    private String externalJobId;
    private String jobTitle;
    private String companyName;
    private String location;
    private String cvDocumentId;
    private String coverLetterDocumentId;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime appliedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getCanonicalJobId() { return canonicalJobId; }
    public void setCanonicalJobId(String canonicalJobId) { this.canonicalJobId = canonicalJobId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getExternalJobId() { return externalJobId; }
    public void setExternalJobId(String externalJobId) { this.externalJobId = externalJobId; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCvDocumentId() { return cvDocumentId; }
    public void setCvDocumentId(String cvDocumentId) { this.cvDocumentId = cvDocumentId; }
    public String getCoverLetterDocumentId() { return coverLetterDocumentId; }
    public void setCoverLetterDocumentId(String coverLetterDocumentId) { this.coverLetterDocumentId = coverLetterDocumentId; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
