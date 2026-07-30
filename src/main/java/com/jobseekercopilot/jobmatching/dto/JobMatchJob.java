package com.jobseekercopilot.jobmatching.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobMatchJob {
    private String id;
    private String canonicalJobId;
    private String provider;
    private String primarySource;
    private String externalJobId;
    private String title;
    private String jobTitle;
    private String company;
    private String companyName;
    private String location;
    private Object canonicalLocation;
    private Object salary;
    private String employmentType;
    private String postedDate;
    private String description;
    private String url;
    private String sourceUrl;
    private List<JobSourceReference> sources = new ArrayList<>();
    private Double distanceMiles;
    private Double matchScore;
    private ApplicationStatus applicationStatus;
    private UUID applicationId;
    private String cvDocumentId;
    private String coverLetterDocumentId;
    private OffsetDateTime appliedAt;
    private OffsetDateTime applicationUpdatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCanonicalJobId() { return canonicalJobId; }
    public void setCanonicalJobId(String canonicalJobId) { this.canonicalJobId = canonicalJobId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getPrimarySource() { return primarySource; }
    public void setPrimarySource(String primarySource) { this.primarySource = primarySource; }
    public String getExternalJobId() { return externalJobId; }
    public void setExternalJobId(String externalJobId) { this.externalJobId = externalJobId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Object getCanonicalLocation() { return canonicalLocation; }
    public void setCanonicalLocation(Object canonicalLocation) { this.canonicalLocation = canonicalLocation; }
    public Object getSalary() { return salary; }
    public void setSalary(Object salary) { this.salary = salary; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public String getPostedDate() { return postedDate; }
    public void setPostedDate(String postedDate) { this.postedDate = postedDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public List<JobSourceReference> getSources() { return sources; }
    public void setSources(List<JobSourceReference> sources) { this.sources = sources == null ? new ArrayList<>() : sources; }
    public Double getDistanceMiles() { return distanceMiles; }
    public void setDistanceMiles(Double distanceMiles) { this.distanceMiles = distanceMiles; }
    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }
    public ApplicationStatus getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(ApplicationStatus applicationStatus) { this.applicationStatus = applicationStatus; }
    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }
    public String getCvDocumentId() { return cvDocumentId; }
    public void setCvDocumentId(String cvDocumentId) { this.cvDocumentId = cvDocumentId; }
    public String getCoverLetterDocumentId() { return coverLetterDocumentId; }
    public void setCoverLetterDocumentId(String coverLetterDocumentId) { this.coverLetterDocumentId = coverLetterDocumentId; }
    public OffsetDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(OffsetDateTime appliedAt) { this.appliedAt = appliedAt; }
    public OffsetDateTime getApplicationUpdatedAt() { return applicationUpdatedAt; }
    public void setApplicationUpdatedAt(OffsetDateTime applicationUpdatedAt) { this.applicationUpdatedAt = applicationUpdatedAt; }
}
