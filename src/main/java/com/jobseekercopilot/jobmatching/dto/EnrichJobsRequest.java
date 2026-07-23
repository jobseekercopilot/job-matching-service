package com.jobseekercopilot.jobmatching.dto;

import java.util.List;

public class EnrichJobsRequest {
    private String userId;
    private List<JobMatchJob> jobs;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<JobMatchJob> getJobs() { return jobs; }
    public void setJobs(List<JobMatchJob> jobs) { this.jobs = jobs; }
}
