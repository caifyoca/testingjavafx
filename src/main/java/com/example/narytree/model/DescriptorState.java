package com.example.narytree.model;

import java.sql.Timestamp;

public class DescriptorState {
    private int id;
    private int descriptorId;
    private String stateValue; // Stores "present", "unknown", "inapplicable" or numeric value as string
    private Timestamp lastModified;

    public DescriptorState() {
    }

    public DescriptorState(int descriptorId, String stateValue) {
        this.descriptorId = descriptorId;
        this.stateValue = stateValue;
    }

    public DescriptorState(int id, int descriptorId, String stateValue, Timestamp lastModified) {
        this.id = id;
        this.descriptorId = descriptorId;
        this.stateValue = stateValue;
        this.lastModified = lastModified;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDescriptorId() {
        return descriptorId;
    }

    public void setDescriptorId(int descriptorId) {
        this.descriptorId = descriptorId;
    }

    public String getStateValue() {
        return stateValue;
    }

    public void setStateValue(String stateValue) {
        this.stateValue = stateValue;
    }

    public Timestamp getLastModified() {
        return lastModified;
    }

    public void setLastModified(Timestamp lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "DescriptorState{" +
               "id=" + id +
               ", descriptorId=" + descriptorId +
               ", stateValue='" + stateValue + '\'' +
               ", lastModified=" + lastModified +
               '}';
    }
}
