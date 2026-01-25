package com.example.barber4u.common;

import androidx.annotation.NonNull;

public class MessageItem {
    @NonNull public final String id;
    @NonNull public final String text;
    @NonNull public final String appointmentId;
    @NonNull public final String barberId;
    @NonNull public final String barberName;

    @NonNull public final String type;   // "APPOINTMENT_NEW" / "APPOINTMENT_STATUS" / "RATE_REQUEST"
    public final boolean seen;

    public MessageItem(
            @NonNull String id,
            @NonNull String text,
            @NonNull String appointmentId,
            @NonNull String barberId,
            @NonNull String barberName,
            @NonNull String type,
            boolean seen
    ) {
        this.id = id;
        this.text = text;
        this.appointmentId = appointmentId;
        this.barberId = barberId;
        this.barberName = barberName;
        this.type = type;
        this.seen = seen;
    }
}