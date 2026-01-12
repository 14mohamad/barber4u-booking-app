package com.example.barber4u.models;

import androidx.annotation.NonNull;

/**
 * מודל משתמש עבור Firebase
 * נשמר בקולקציה: users/{uid}
 */
public class User {

    // תפקידי משתמש במערכת
    public enum Role {
        CUSTOMER,
        BARBER,
        ADMIN
    }

    private String uid;
    private String name;
    private String email;
    private Role role;

    /**
     * בנאי ריק – חובה ל-Firebase
     */
    public User() {
    }

    /**
     * בנאי מלא
     */
    public User(String uid, String name, String email, Role role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // ===== Getters & Setters =====

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}
