package com.example.barber4u.data.repositories;

import androidx.annotation.NonNull;

import com.example.barber4u.common.MessageItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MessagesRepository {

    public interface MessagesListener {
        void onSuccess(@NonNull List<MessageItem> items);
        void onError(@NonNull Exception e);
    }

    public ListenerRegistration listenToMessages(
            @NonNull String uid,
            @NonNull MessagesListener listener
    ) {
        return FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        listener.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<MessageItem> out = new ArrayList<>();

                    snapshot.getDocuments().forEach(doc -> {
                        String id = doc.getId();

                        // ✅ Correct fields
                        String type = safe(doc.getString("type"));
                        String text = safe(doc.getString("text"));

                        String appointmentId = safe(doc.getString("appointmentId"));
                        String barberId = safe(doc.getString("barberId"));
                        String barberName = safe(doc.getString("barberName"));

                        // ✅ Read "seen" from Firestore (default false)
                        Boolean seenObj = doc.getBoolean("seen");
                        boolean seen = seenObj != null && seenObj;

                        out.add(new MessageItem(
                                id,
                                text,
                                appointmentId,
                                barberId,
                                barberName,
                                type,
                                seen
                        ));
                    });

                    listener.onSuccess(out);
                });
    }


    public void dismissMessage(@NonNull String uid, @NonNull String messageId) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("messages")
                .document(messageId)
                .delete();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
