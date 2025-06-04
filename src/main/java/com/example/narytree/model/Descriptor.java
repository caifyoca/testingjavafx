package com.example.narytree.model;

public class Descriptor {
    private int id;
    private String label;
    private Integer parentId; // Use Integer to allow null for root descriptors
    private boolean isNumeric;
    private boolean isMultistate;
    private boolean isTypeOfPresence;
    private boolean isHiddenDefaultly;

    // Constructors (default and parameterized)
    public Descriptor() {}

    public Descriptor(int id, String label, Integer parentId, boolean isNumeric, boolean isMultistate, boolean isTypeOfPresence, boolean isHiddenDefaultly) {
        this.id = id;
        this.label = label;
        this.parentId = parentId;
        this.isNumeric = isNumeric;
        this.isMultistate = isMultistate;
        this.isTypeOfPresence = isTypeOfPresence;
        this.isHiddenDefaultly = isHiddenDefaultly;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public boolean isNumeric() { return isNumeric; }
    public void setNumeric(boolean numeric) { isNumeric = numeric; }

    public boolean isMultistate() { return isMultistate; }
    public void setMultistate(boolean multistate) { isMultistate = multistate; }

    public boolean isTypeOfPresence() { return isTypeOfPresence; }
    public void setTypeOfPresence(boolean typeOfPresence) { isTypeOfPresence = typeOfPresence; }

    public boolean isHiddenDefaultly() { return isHiddenDefaultly; }
    public void setHiddenDefaultly(boolean hiddenDefaultly) { isHiddenDefaultly = hiddenDefaultly; }

    @Override
    public String toString() {
        return "Descriptor{" +
               "id=" + id +
               ", label='" + label + '\'' +
               ", parentId=" + parentId +
               // ... other fields
               '}';
    }
}
