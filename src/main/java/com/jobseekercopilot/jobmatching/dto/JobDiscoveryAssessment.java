package com.jobseekercopilot.jobmatching.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

public class JobDiscoveryAssessment {
    private String algorithmVersion;
    @Schema(allowableValues = {"OPEN_AT_RETRIEVAL", "UNKNOWN", "CLOSED", "EXPIRED"})
    private String availability;
    @Schema(allowableValues = {"VACANCY", "APPRENTICESHIP", "PAID_TRAINING"})
    private String engagementType;
    @Schema(allowableValues = {
            "SOFTWARE", "DATA", "QUALITY_ENGINEERING", "CYBER_SECURITY",
            "DESIGN", "PRODUCT_PROJECT", "IT_SUPPORT", "NON_TECHNICAL", "UNKNOWN"
    })
    private String occupationFamily;
    @Schema(allowableValues = {"JUNIOR_ENTRY", "MID", "SENIOR", "LEADERSHIP", "UNSPECIFIED"})
    private String seniority;
    @Schema(allowableValues = {"ALIGNED", "RELATED", "MISMATCHED", "UNKNOWN"})
    private String targetRoleAlignment;
    private String targetRole;
    private boolean excluded;
    private List<String> exclusionReasons = new ArrayList<>();
    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String value) { algorithmVersion = value; }
    public String getAvailability() { return availability; }
    public void setAvailability(String value) { availability = value; }
    public String getEngagementType() { return engagementType; }
    public void setEngagementType(String value) { engagementType = value; }
    public String getOccupationFamily() { return occupationFamily; }
    public void setOccupationFamily(String value) { occupationFamily = value; }
    public String getSeniority() { return seniority; }
    public void setSeniority(String value) { seniority = value; }
    public String getTargetRoleAlignment() { return targetRoleAlignment; }
    public void setTargetRoleAlignment(String value) { targetRoleAlignment = value; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String value) { targetRole = value; }
    public boolean isExcluded() { return excluded; }
    public void setExcluded(boolean value) { excluded = value; }
    public List<String> getExclusionReasons() { return exclusionReasons; }
    public void setExclusionReasons(List<String> value) { exclusionReasons = value == null ? new ArrayList<>() : value; }
}
