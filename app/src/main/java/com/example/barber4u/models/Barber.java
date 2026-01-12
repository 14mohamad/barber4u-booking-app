package com.example.barber4u.models;

import java.util.List;

public class Barber {
    private String id;
    private String name;
    private List<String> branchIds;   // ✅ ספר יכול לעבוד בכמה סניפים
    private boolean active;

    public Barber() {}

    public Barber(String id, String name, List<String> branchIds, boolean active) {
        this.id = id;
        this.name = name;
        this.branchIds = branchIds;
        this.active = active;
    }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public List<String> getBranchIds() { return branchIds; }

    public void setBranchIds(List<String> branchIds) { this.branchIds = branchIds; }

    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return name;
    }
}