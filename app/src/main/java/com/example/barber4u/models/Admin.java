package com.example.barber4u.models;

public class Admin extends User {

    // חובה ל-Firebase
    public Admin() {
        super();
    }

    public Admin(String uid, String name, String email) {
        super(uid, name, email);
    }
}
