package com.example.barber4u.data.repositories;

import androidx.annotation.NonNull;

import com.example.barber4u.data.firebase.FirebaseProvider;
import com.example.barber4u.models.User;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserRepository {

    public interface UserCallback {
        void onSuccess(@NonNull DocumentSnapshot doc);
        void onError(@NonNull Exception e);
    }

    public void getUserById(@NonNull String uid, @NonNull UserCallback callback) {
        FirebaseProvider.db()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onError);
    }

    @NonNull
    public User.Role parseRole(@NonNull DocumentSnapshot doc) {
        String roleString = doc.getString("role");
        if (roleString == null) return User.Role.CUSTOMER;

        try {
            return User.Role.valueOf(roleString.trim().toUpperCase());
        } catch (Exception e) {
            return User.Role.CUSTOMER;
        }
    }
}