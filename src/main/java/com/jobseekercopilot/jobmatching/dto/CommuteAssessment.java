package com.jobseekercopilot.jobmatching.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommuteAssessment {
    public enum Status { WITHIN_PREFERENCE, ABOVE_PREFERENCE, UNAVAILABLE, NOT_APPLICABLE, NOT_EVALUATED }
    public enum EstimateStatus { ESTIMATED, APPROXIMATE, UNAVAILABLE }
    public enum Outcome { WITHIN, ABOVE, NO_PREFERENCE, UNAVAILABLE }

    private Status status;
    private String workplaceType;
    private List<ModeAssessment> modes = new ArrayList<>();
    private CommuteTravelMode bestSuitableMode;
    private String explanationCode;
    private String providerAttribution;

    public Status getStatus() { return status; }
    public void setStatus(Status value) { status = value; }
    public String getWorkplaceType() { return workplaceType; }
    public void setWorkplaceType(String value) { workplaceType = value; }
    public List<ModeAssessment> getModes() { return modes; }
    public void setModes(List<ModeAssessment> value) { modes = value == null ? new ArrayList<>() : value; }
    public CommuteTravelMode getBestSuitableMode() { return bestSuitableMode; }
    public void setBestSuitableMode(CommuteTravelMode value) { bestSuitableMode = value; }
    public String getExplanationCode() { return explanationCode; }
    public void setExplanationCode(String value) { explanationCode = value; }
    public String getProviderAttribution() { return providerAttribution; }
    public void setProviderAttribution(String value) { providerAttribution = value; }

    public static class ModeAssessment {
        private CommuteTravelMode mode;
        private EstimateStatus estimateStatus;
        private Integer durationMinutes;
        private BigDecimal distanceMiles;
        private Integer thresholdMinutes;
        private Outcome outcome;
        private OffsetDateTime calculatedFor;
        private String originPrecision;
        private String destinationPrecision;
        private String reasonCode;

        public CommuteTravelMode getMode() { return mode; }
        public void setMode(CommuteTravelMode value) { mode = value; }
        public EstimateStatus getEstimateStatus() { return estimateStatus; }
        public void setEstimateStatus(EstimateStatus value) { estimateStatus = value; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer value) { durationMinutes = value; }
        public BigDecimal getDistanceMiles() { return distanceMiles; }
        public void setDistanceMiles(BigDecimal value) { distanceMiles = value; }
        public Integer getThresholdMinutes() { return thresholdMinutes; }
        public void setThresholdMinutes(Integer value) { thresholdMinutes = value; }
        public Outcome getOutcome() { return outcome; }
        public void setOutcome(Outcome value) { outcome = value; }
        public OffsetDateTime getCalculatedFor() { return calculatedFor; }
        public void setCalculatedFor(OffsetDateTime value) { calculatedFor = value; }
        public String getOriginPrecision() { return originPrecision; }
        public void setOriginPrecision(String value) { originPrecision = value; }
        public String getDestinationPrecision() { return destinationPrecision; }
        public void setDestinationPrecision(String value) { destinationPrecision = value; }
        public String getReasonCode() { return reasonCode; }
        public void setReasonCode(String value) { reasonCode = value; }
    }
}
