package com.example.narytree.model;

public class ApplicabilityRule {
    private int id;
    private int sourceId;
    private int targetId;
    private boolean isApplicableIf;

    // Constructors
    public ApplicabilityRule() {}

    public ApplicabilityRule(int id, int sourceId, int targetId, boolean isApplicableIf) {
        this.id = id;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.isApplicableIf = isApplicableIf;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSourceId() { return sourceId; }
    public void setSourceId(int sourceId) { this.sourceId = sourceId; }

    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }

    public boolean isApplicableIf() { return isApplicableIf; }
    public void setApplicableIf(boolean applicableIf) { isApplicableIf = applicableIf; }

    @Override
    public String toString() {
        return "ApplicabilityRule{" +
               "id=" + id +
               ", sourceId=" + sourceId +
               ", targetId=" + targetId +
               ", isApplicableIf=" + isApplicableIf +
               '}';
    }
}
