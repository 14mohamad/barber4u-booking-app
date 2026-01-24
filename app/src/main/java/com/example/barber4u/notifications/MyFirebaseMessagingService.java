package com.example.barber4u.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.barber4u.R;
import com.example.barber4u.main.RoleMainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String CHANNEL_ID = "appointments_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        // ✅ If user is logged in, update token in Firestore
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(data, SetOptions.merge());
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        // Prefer data payload (your function uses data + notification)
        Map<String, String> data = message.getData();
        if (data == null || data.isEmpty()) return;

        String type = data.get("type");
        if (type == null) return;

        // Your backend currently sends type: "RATE_REQUEST"
        if ("RATE_REQUEST".equals(type)) {
            String barberName = safe(data.get("barberName"));
            showNotification(
                    "Rate your haircut",
                    barberName.isEmpty()
                            ? "Your appointment is done. Tap to rate."
                            : "Your appointment with " + barberName + " is done. Tap to rate.",
                    data
            );
            return;
        }

        // If you still use APPOINTMENT_DONE somewhere:
        if ("APPOINTMENT_DONE".equals(type)) {
            String barberName = safe(data.get("barberName"));
            showNotification(
                    "Appointment completed",
                    barberName.isEmpty()
                            ? "Your appointment is done."
                            : "Your appointment with " + barberName + " is done.",
                    data
            );
        }
    }

    private void showNotification(@NonNull String title,
                                  @NonNull String body,
                                  @NonNull Map<String, String> data) {

        createNotificationChannel();

        // Tap -> open app (RoleMainActivity)
        Intent intent = new Intent(this, RoleMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Pass info if you want to open Messages screen later
        intent.putExtra("push_type", safe(data.get("type")));
        intent.putExtra("appointmentId", safe(data.get("appointmentId")));
        intent.putExtra("barberName", safe(data.get("barberName")));
        intent.putExtra("branchName", safe(data.get("branchName")));

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        // Android 13+ runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Appointments",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Appointment notifications");

        manager.createNotificationChannel(channel);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
