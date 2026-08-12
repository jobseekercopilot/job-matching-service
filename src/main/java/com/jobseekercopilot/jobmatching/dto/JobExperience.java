package com.jobseekercopilot.jobmatching.dto;

import java.math.BigDecimal;

public class JobExperience {
    private String rawValue;
    private String level;
    private BigDecimal minimumYears;
    private BigDecimal maximumYears;
    public String getRawValue() { return rawValue; }
    public void setRawValue(String value) { rawValue = value; }
    public String getLevel() { return level; }
    public void setLevel(String value) { level = value; }
    public BigDecimal getMinimumYears() { return minimumYears; }
    public void setMinimumYears(BigDecimal value) { minimumYears = value; }
    public BigDecimal getMaximumYears() { return maximumYears; }
    public void setMaximumYears(BigDecimal value) { maximumYears = value; }
}
