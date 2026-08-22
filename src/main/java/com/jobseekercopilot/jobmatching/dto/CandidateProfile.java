package com.jobseekercopilot.jobmatching.dto;

import java.util.ArrayList;
import java.util.List;

public class CandidateProfile {
    private List<String> skills = new ArrayList<>();
    private List<CandidateRole> roles = new ArrayList<>();
    private List<CandidateQualification> qualifications = new ArrayList<>();
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> value) { skills = value == null ? new ArrayList<>() : value; }
    public List<CandidateRole> getRoles() { return roles; }
    public void setRoles(List<CandidateRole> value) { roles = value == null ? new ArrayList<>() : value; }
    public List<CandidateQualification> getQualifications() { return qualifications; }
    public void setQualifications(List<CandidateQualification> value) { qualifications = value == null ? new ArrayList<>() : value; }
}
