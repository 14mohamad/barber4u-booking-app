package com.example.barber4u.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.barber4u.data.repositories.MessagesRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessagesViewModel extends ViewModel {

    private final MessagesRepository repo = new MessagesRepository();

    private final MutableLiveData<List<MessageItem>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);

    private ListenerRegistration registration;

    public LiveData<List<MessageItem>> messages() { return messages; }
    public LiveData<Boolean> loading() { return loading; }
    public LiveData<String> error() { return error; }

    public void start() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            messages.setValue(new ArrayList<>());
            loading.setValue(false);
            error.setValue(null);
            return;
        }

        stop(); // prevent duplicates
        loading.setValue(true);
        error.setValue(null);

        registration = repo.listenToMessages(user.getUid(), new MessagesRepository.MessagesListener() {
            @Override
            public void onSuccess(@NonNull List<MessageItem> items) {
                loading.postValue(false);
                messages.postValue(items);
            }

            @Override
            public void onError(@NonNull Exception e) {
                loading.postValue(false);
                error.postValue(e.getMessage());
                messages.postValue(new ArrayList<>());
            }
        });
    }

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    public void dismiss(@NonNull MessageItem item) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        repo.dismissMessage(user.getUid(), item.id);
    }

    /**
     * Rating logic is in ViewModel (business logic layer),
     * Fragment only shows dialog and passes (item, rating).
     */
    public void submitRating(
            @NonNull MessageItem msg,
            int rating,
            @NonNull Runnable onSuccess,
            @NonNull java.util.function.Consumer<String> onError
    ) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            onError.accept("Not logged in");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        var apptRef = db.collection("appointments").document(msg.appointmentId);

        // 🔴 STEP 1: try ONLY updating appointment
        apptRef.update(
                "rating", rating,
                "ratedAt", FieldValue.serverTimestamp()
        ).addOnSuccessListener(unused -> {

            // 🟢 Appointment update OK — now try deleting message
            db.collection("users")
                    .document(user.getUid())
                    .collection("messages")
                    .document(msg.id)
                    .delete()
                    .addOnSuccessListener(unused2 -> {
                        onSuccess.run();
                    })
                    .addOnFailureListener(e2 -> {
                        onError.accept("Delete message failed: " + e2.getMessage());
                    });

        }).addOnFailureListener(e -> {
            onError.accept("Update appointment failed: " + e.getMessage());
        });
    }


    @Override
    protected void onCleared() {
        stop();
        super.onCleared();
    }
}
