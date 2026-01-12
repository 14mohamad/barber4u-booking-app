package com.example.barber4u.models;

public class Appointment {
    private String id;
    private String userEmail;
    private String userId;
    private String date;
    private String time;
    private String status;
    private String branchName;
    private String barberName;
    private String galleryItemId;
    private String galleryImageUrl;
    private String galleryTitle;

    public Appointment() {
        // חובה בשביל Firestore
    }

    public Appointment(String id, String userEmail, String userId,
                       String date, String time,
                       String status, String branchName, String barberName) {
        this.id = id;
        this.userEmail = userEmail;
        this.userId = userId;
        this.date = date;
        this.time = time;
        this.status = status;
        this.branchName = branchName;
        this.barberName = barberName;
    }

    public String getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserId() {
        return userId;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getBarberName() {
        return barberName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGalleryItemId() { return galleryItemId; }
    public void setGalleryItemId(String galleryItemId) { this.galleryItemId = galleryItemId; }

    public String getGalleryImageUrl() { return galleryImageUrl; }
    public void setGalleryImageUrl(String galleryImageUrl) { this.galleryImageUrl = galleryImageUrl; }

    public String getGalleryTitle() { return galleryTitle; }
    public void setGalleryTitle(String galleryTitle) { this.galleryTitle = galleryTitle; }
}
