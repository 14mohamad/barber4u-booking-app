package com.example.barber4u.common;

import com.google.firebase.Timestamp;

public class RatingPayload {
    public String userId;
    public int rating;
    public Timestamp createdAt = Timestamp.now();

    RatingPayload(String userId, int rating) {
        this.userId = userId;
        this.rating = rating;
    }
}
