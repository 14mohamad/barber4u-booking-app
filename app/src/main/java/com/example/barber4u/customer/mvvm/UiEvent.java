package com.example.barber4u.customer.mvvm;

public class UiEvent {
    public enum Type { TOAST }

    public final Type type;
    public final String message;

    private UiEvent(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public static UiEvent toast(String msg) {
        return new UiEvent(Type.TOAST, msg);
    }
}