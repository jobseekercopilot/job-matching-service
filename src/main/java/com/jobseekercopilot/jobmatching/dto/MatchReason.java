package com.jobseekercopilot.jobmatching.dto;

public class MatchReason {
    private String code;
    private String severity;
    private String status;
    private String explanation;
    public MatchReason() {}
    public MatchReason(String code, String severity, String status, String explanation) {
        this.code = code; this.severity = severity; this.status = status; this.explanation = explanation;
    }
    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public String getSeverity() { return severity; }
    public void setSeverity(String value) { severity = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String value) { explanation = value; }
}
