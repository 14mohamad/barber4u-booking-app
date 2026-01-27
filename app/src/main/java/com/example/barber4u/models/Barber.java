package com.example.barber4u.models;

import java.util.List;

public class Barber extends User {

    private List<String> branchIds;   // ספר יכול לעבוד בכמה סניפים
    private boolean active;

    // חובה ל-Firebase
    public Barber() {
        super();
    }

    public Barber(String uid,
                  String name,
                  String email,
                  List<String> branchIds,
                  boolean active) {
        super(uid, name, email);
        this.branchIds = branchIds;
        this.active = active;
    }

    // ===== Getters & Setters =====

    public List<String> getBranchIds() {
        return branchIds;
    }

    public void setBranchIds(List<String> branchIds) {
        this.branchIds = branchIds;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return (name != null && !name.trim().isEmpty()) ? name : "Barber";
    }


}
