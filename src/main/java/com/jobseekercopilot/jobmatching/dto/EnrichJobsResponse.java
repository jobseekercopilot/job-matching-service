package com.jobseekercopilot.jobmatching.dto;

import java.util.List;

public class EnrichJobsResponse {
    private String userId;
    private List<JobMatchJob> jobs;

    public EnrichJobsResponse() {
    }

    public EnrichJobsResponse(String userId, List<JobMatchJob> jobs) {
        this.userId = userId;
        this.jobs = jobs;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<JobMatchJob> getJobs() { return jobs; }
    public void setJobs(List<JobMatchJob> jobs) { this.jobs = jobs; }
}
