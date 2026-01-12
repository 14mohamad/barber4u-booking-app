// Branch.java
package com.example.barber4u.models;

import com.google.firebase.firestore.GeoPoint;

public class Branch {
    private String id;        // docId: branch1/branch2/...
    private String branchId;  // שדה בתוך המסמך (נוח אבל לא חובה)
    private String name;
    private String address;
    private String phone;
    private boolean active;
    private GeoPoint location;

    public Branch() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBranchId() {
        // אם לא שמור במסמך – נחזיר את ה-id
        return (branchId == null || branchId.trim().isEmpty()) ? id : branchId;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public boolean isActive() { return active; }

    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }

    @Override
    public String toString() {
        // מה שמופיע ב-Spinner
        return name + " (" + getBranchId() + ")";
    }
}