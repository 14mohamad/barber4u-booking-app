package com.example.barber4u.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.barber4u.data.firebase.FirebaseProvider;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserRepository {

    public interface UserCallback {
        void onSuccess(@NonNull DocumentSnapshot doc);
        void onError(@NonNull Exception e);
    }

    // Keep roles as Strings so your model refactor (abstract User + subclasses) won't break this
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_BARBER   = "BARBER";
    public static final String ROLE_ADMIN    = "ADMIN";

    public void getUserById(@NonNull String uid, @NonNull UserCallback callback) {
        FirebaseProvider.db()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onError);
    }

    /**
     * Returns normalized role string: CUSTOMER / BARBER / ADMIN.
     * Defaults to CUSTOMER if missing/invalid.
     */
    @NonNull
    public String parseRole(@NonNull DocumentSnapshot doc) {
        String roleString = doc.getString("role");
        String norm = normalizeRole(roleString);

        if (ROLE_ADMIN.equals(norm) || ROLE_BARBER.equals(norm) || ROLE_CUSTOMER.equals(norm)) {
            return norm;
        }
        return ROLE_CUSTOMER;
    }

    public boolean isCustomer(@NonNull DocumentSnapshot doc) {
        return ROLE_CUSTOMER.equals(parseRole(doc));
    }

    public boolean isBarber(@NonNull DocumentSnapshot doc) {
        return ROLE_BARBER.equals(parseRole(doc));
    }

    public boolean isAdmin(@NonNull DocumentSnapshot doc) {
        return ROLE_ADMIN.equals(parseRole(doc));
    }

    @NonNull
    private static String normalizeRole(@Nullable String role) {
        if (role == null) return ROLE_CUSTOMER;
        return role.trim().toUpperCase();
    }
}
