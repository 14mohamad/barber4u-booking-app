package com.example.barber4u.customer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.barber4u.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class HomeCustomerFragment extends Fragment {

    private TextView tvGreeting, tvSubTitle;
    private TextView tvStatTotal, tvStatUpcoming, tvStatCanceled;
    private TextView tvNextBarber, tvNextDate, tvNextStatus;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public HomeCustomerFragment() {
        // חובה קונסטרקטור ריק
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_home_customer, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // קישור ל־Views מה־XML
        tvGreeting       = view.findViewById(R.id.tvGreeting);
        tvSubTitle       = view.findViewById(R.id.tvSubTitle);
        tvStatTotal      = view.findViewById(R.id.tvStatTotal);
        tvStatUpcoming   = view.findViewById(R.id.tvStatUpcoming);
        tvStatCanceled   = view.findViewById(R.id.tvStatCanceled);
        tvNextBarber     = view.findViewById(R.id.tvNextBarber);
        tvNextDate       = view.findViewById(R.id.tvNextDate);
        tvNextStatus     = view.findViewById(R.id.tvNextStatus);

        // שם מה־Intent (מה־LoginActivity)
        String name = null;
        if (getActivity() != null && getActivity().getIntent() != null) {
            name = getActivity().getIntent().getStringExtra("userName");
        }
        if (name == null || name.trim().isEmpty()) {
            name = "Customer";
        }

        tvGreeting.setText("Hi, " + name);
        tvSubTitle.setText("Welcome back 👋");

        loadAppointmentsForUser();
    }

    private void loadAppointmentsForUser() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("appointments")
                .whereEqualTo("userId", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(getContext(),
                                "Failed to load appointments",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot == null || snapshot.isEmpty()) {
                        // אין תורים – נשאיר 0 ו־"-"
                        tvStatTotal.setText("0");
                        tvStatUpcoming.setText("0");
                        tvStatCanceled.setText("0");

                        tvNextBarber.setText("Barber: -");
                        tvNextDate.setText("Date: -");
                        tvNextStatus.setText("Status: -");
                        return;
                    }

                    int total = snapshot.size();
                    int upcoming = 0;
                    int canceled = 0;

                    // "התור הבא" – פשוט ניקח את המסמך הראשון במצב PENDING/APPROVED
                    DocumentSnapshot nextDoc = null;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if (status == null) status = "";

                        if (status.equalsIgnoreCase("CANCELED")) {
                            canceled++;
                        } else {
                            upcoming++;
                        }

                        if (nextDoc == null &&
                                (status.equalsIgnoreCase("PENDING")
                                        || status.equalsIgnoreCase("APPROVED"))) {
                            nextDoc = doc;
                        }
                    }

                    tvStatTotal.setText(String.valueOf(total));
                    tvStatUpcoming.setText(String.valueOf(upcoming));
                    tvStatCanceled.setText(String.valueOf(canceled));

                    if (nextDoc != null) {
                        String barberName = nextDoc.getString("barberName");
                        String date = nextDoc.getString("date");
                        String time = nextDoc.getString("time");
                        String status = nextDoc.getString("status");

                        tvNextBarber.setText("Barber: " + (barberName != null ? barberName : "-"));
                        tvNextDate.setText("Date: " + (date != null ? date : "-")
                                + (time != null ? " " + time : ""));
                        tvNextStatus.setText("Status: " + (status != null ? status : "-"));
                    } else {
                        tvNextBarber.setText("Barber: -");
                        tvNextDate.setText("Date: -");
                        tvNextStatus.setText("Status: -");
                    }
                });
    }
}
