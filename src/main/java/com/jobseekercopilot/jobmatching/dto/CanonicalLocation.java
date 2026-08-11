package com.jobseekercopilot.jobmatching.dto;

import java.math.BigDecimal;

public class CanonicalLocation {
    private String locationId;
    private String displayName;
    private String postcode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String countryCode;
    private String locationType;
    private String precision;
    private String confidence;

    public String getLocationId() { return locationId; }
    public void setLocationId(String value) { locationId = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public String getPostcode() { return postcode; }
    public void setPostcode(String value) { postcode = value; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal value) { latitude = value; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal value) { longitude = value; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String value) { countryCode = value; }
    public String getLocationType() { return locationType; }
    public void setLocationType(String value) { locationType = value; }
    public String getPrecision() { return precision; }
    public void setPrecision(String value) { precision = value; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String value) { confidence = value; }
}
