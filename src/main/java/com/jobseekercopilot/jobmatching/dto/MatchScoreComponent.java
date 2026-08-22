package com.jobseekercopilot.jobmatching.dto;

public class MatchScoreComponent {
    private String code;
    private Double score;
    private Double maximumScore;
    private String explanation;
    public MatchScoreComponent() {}
    public MatchScoreComponent(String code, Double score, Double maximumScore, String explanation) {
        this.code = code; this.score = score; this.maximumScore = maximumScore; this.explanation = explanation;
    }
    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public Double getScore() { return score; }
    public void setScore(Double value) { score = value; }
    public Double getMaximumScore() { return maximumScore; }
    public void setMaximumScore(Double value) { maximumScore = value; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String value) { explanation = value; }
}
