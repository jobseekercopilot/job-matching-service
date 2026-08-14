package com.jobseekercopilot.jobmatching.dto;

import java.util.ArrayList;
import java.util.List;

public class MatchAssessment {
    private Double score;
    private String provenance;
    private String algorithmVersion;
    private String targetRole;
    private String rating;
    private boolean candidateProfileUsed;
    private List<MatchScoreComponent> components = new ArrayList<>();
    private List<MatchReason> reasons = new ArrayList<>();
    private List<MatchReason> hardGateReasons = new ArrayList<>();
    public Double getScore() { return score; }
    public void setScore(Double value) { score = value; }
    public String getProvenance() { return provenance; }
    public void setProvenance(String value) { provenance = value; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String value) { algorithmVersion = value; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String value) { targetRole = value; }
    public String getRating() { return rating; }
    public void setRating(String value) { rating = value; }
    public boolean isCandidateProfileUsed() { return candidateProfileUsed; }
    public void setCandidateProfileUsed(boolean value) { candidateProfileUsed = value; }
    public List<MatchScoreComponent> getComponents() { return components; }
    public void setComponents(List<MatchScoreComponent> value) { components = value == null ? new ArrayList<>() : value; }
    public List<MatchReason> getReasons() { return reasons; }
    public void setReasons(List<MatchReason> value) { reasons = value == null ? new ArrayList<>() : value; }
    public List<MatchReason> getHardGateReasons() { return hardGateReasons; }
    public void setHardGateReasons(List<MatchReason> value) { hardGateReasons = value == null ? new ArrayList<>() : value; }
}
