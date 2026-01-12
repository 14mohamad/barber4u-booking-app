package com.example.barber4u.models;

public class GalleryItem {
    private String id;
    private String barberId;
    private String branchId;
    private String title;
    private String imageUrl;
    private Boolean active;

    public GalleryItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBarberId() { return barberId; }
    public void setBarberId(String barberId) { this.barberId = barberId; }

    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    @Override
    public String toString() {
        return title == null ? "" : title;
    }
}