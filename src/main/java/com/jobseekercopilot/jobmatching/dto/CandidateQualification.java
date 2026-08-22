package com.jobseekercopilot.jobmatching.dto;

public class CandidateQualification {
    private String qualificationName;
    private String status;
    private String dateAchieved;
    private String expectedCompletion;
    public String getQualificationName() { return qualificationName; }
    public void setQualificationName(String value) { qualificationName = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getDateAchieved() { return dateAchieved; }
    public void setDateAchieved(String value) { dateAchieved = value; }
    public String getExpectedCompletion() { return expectedCompletion; }
    public void setExpectedCompletion(String value) { expectedCompletion = value; }
}
