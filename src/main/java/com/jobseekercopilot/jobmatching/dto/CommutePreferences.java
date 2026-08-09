package com.jobseekercopilot.jobmatching.dto;

import java.util.ArrayList;
import java.util.List;

public class CommutePreferences {
    private List<CommuteTravelMode> commuteTravelModes = new ArrayList<>();
    private Integer maximumDrivingMinutes;
    private Integer maximumTransitMinutes;
    private Integer maximumDistanceMiles;
    private List<String> workplaceArrangements = new ArrayList<>();

    public List<CommuteTravelMode> getCommuteTravelModes() { return commuteTravelModes; }
    public void setCommuteTravelModes(List<CommuteTravelMode> value) { commuteTravelModes = value == null ? new ArrayList<>() : value; }
    public Integer getMaximumDrivingMinutes() { return maximumDrivingMinutes; }
    public void setMaximumDrivingMinutes(Integer value) { maximumDrivingMinutes = value; }
    public Integer getMaximumTransitMinutes() { return maximumTransitMinutes; }
    public void setMaximumTransitMinutes(Integer value) { maximumTransitMinutes = value; }
    public Integer getMaximumDistanceMiles() { return maximumDistanceMiles; }
    public void setMaximumDistanceMiles(Integer value) { maximumDistanceMiles = value; }
    public List<String> getWorkplaceArrangements() { return workplaceArrangements; }
    public void setWorkplaceArrangements(List<String> value) { workplaceArrangements = value == null ? new ArrayList<>() : value; }
}
