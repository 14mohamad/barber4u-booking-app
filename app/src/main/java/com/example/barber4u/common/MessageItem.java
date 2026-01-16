package com.example.barber4u.common;

public class MessageItem {

    public final String id;
    public final String text;
    public final String appointmentId;
    public final String barberId;
    public final String barberName;

    public MessageItem(
            String id,
            String text,
            String appointmentId,
            String barberId,
            String barberName
    ) {
        this.id = id;
        this.text = text;
        this.appointmentId = appointmentId;
        this.barberId = barberId;
        this.barberName = barberName;
    }
}