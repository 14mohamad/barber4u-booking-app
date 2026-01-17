package com.example.barber4u.models;

public class Customer extends User {

    // חובה ל-Firebase
    public Customer() {
        super();
    }

    public Customer(String uid, String name, String email) {
        super(uid, name, email);
    }
}
