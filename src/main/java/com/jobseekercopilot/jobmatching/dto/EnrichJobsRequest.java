package com.jobseekercopilot.jobmatching.dto;

import java.util.List;

public class EnrichJobsRequest {
    private String userId;
    private List<JobMatchJob> jobs;
    private HomeLocation homeLocation;
    private CommutePreferences commutePreferences;
    private String targetRole;
    private CandidateProfile candidateProfile;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<JobMatchJob> getJobs() { return jobs; }
    public void setJobs(List<JobMatchJob> jobs) { this.jobs = jobs; }
    public HomeLocation getHomeLocation() { return homeLocation; }
    public void setHomeLocation(HomeLocation value) { homeLocation = value; }
    public CommutePreferences getCommutePreferences() { return commutePreferences; }
    public void setCommutePreferences(CommutePreferences value) { commutePreferences = value; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String value) { targetRole = value; }
    public CandidateProfile getCandidateProfile() { return candidateProfile; }
    public void setCandidateProfile(CandidateProfile value) { candidateProfile = value; }
}
