package com.example.barber4u.barber;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.barber4u.R;
import com.example.barber4u.models.Branch;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarberAddGalleryItemFragment extends Fragment {

    private Spinner spBranch;
    private EditText etTitle, etImageUrl;
    private Button btnSave;
    private ProgressBar progress;

    private FirebaseFirestore db;
    private String barberId;

    private final List<Branch> branchList = new ArrayList<>();
    private ArrayAdapter<Branch> branchAdapter;

    public BarberAddGalleryItemFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_barber_add_gallery_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        barberId = FirebaseAuth.getInstance().getUid();

        spBranch = view.findViewById(R.id.spBranch);
        etTitle = view.findViewById(R.id.etTitle);
        etImageUrl = view.findViewById(R.id.etImageUrl);
        btnSave = view.findViewById(R.id.btnSave);
        progress = view.findViewById(R.id.progress);

        branchAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                branchList
        );
        branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBranch.setAdapter(branchAdapter);

        btnSave.setOnClickListener(v -> saveGalleryItem());

        loadMyBranches();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        spBranch.setEnabled(!loading);
        etTitle.setEnabled(!loading);
        etImageUrl.setEnabled(!loading);
    }

    /**
     * טוען את הסניפים של הספר לפי barbers/{uid}.branchIds (מערך)
     * ואז מביא את ה-branches במסמכים הללו.
     */
    private void loadMyBranches() {
        if (barberId == null) {
            Toast.makeText(requireContext(), "Barber not logged in", Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);
        branchList.clear();
        branchAdapter.notifyDataSetChanged();

        db.collection("barbers")
                .document(barberId)
                .get()
                .addOnSuccessListener(doc -> {
                    List<String> branchIds = (List<String>) doc.get("branchIds");
                    if (branchIds == null || branchIds.isEmpty()) {
                        setLoading(false);
                        Toast.makeText(requireContext(), "אין לך סניפים משוייכים", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // מביא כל סניף לפי docId
                    // (פשוט וברור, בלי אינדקסים)
                    final int[] remaining = {branchIds.size()};

                    for (String bid : branchIds) {
                        db.collection("branches")
                                .document(bid)
                                .get()
                                .addOnSuccessListener(bdoc -> {
                                    if (bdoc.exists()) {
                                        Boolean active = bdoc.getBoolean("active");
                                        if (active == null || active) {
                                            Branch b = bdoc.toObject(Branch.class);
                                            if (b != null) {
                                                b.setId(bdoc.getId());
                                                branchList.add(b);
                                            }
                                        }
                                    }
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        branchAdapter.notifyDataSetChanged();
                                        setLoading(false);

                                        if (branchList.isEmpty()) {
                                            Toast.makeText(requireContext(), "אין סניפים פעילים", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        branchAdapter.notifyDataSetChanged();
                                        setLoading(false);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "שגיאה בטעינת סניפים: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveGalleryItem() {
        if (barberId == null) {
            Toast.makeText(requireContext(), "Barber not logged in", Toast.LENGTH_LONG).show();
            return;
        }

        if (branchList.isEmpty()) {
            Toast.makeText(requireContext(), "אין סניפים לבחירה", Toast.LENGTH_LONG).show();
            return;
        }

        Branch branch = (Branch) spBranch.getSelectedItem();
        if (branch == null || branch.getId() == null) {
            Toast.makeText(requireContext(), "בחר סניף", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String imageUrl = etImageUrl.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("חובה");
            etTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(imageUrl)) {
            etImageUrl.setError("חובה");
            etImageUrl.requestFocus();
            return;
        }

        // בדיקת URL בסיסית
        if (!Patterns.WEB_URL.matcher(imageUrl).matches()) {
            etImageUrl.setError("URL לא תקין");
            etImageUrl.requestFocus();
            return;
        }

        setLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("active", true);
        data.put("barberId", barberId);
        data.put("branchId", branch.getId());
        data.put("title", title);
        data.put("imageUrl", imageUrl);
        data.put("createdAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());

        db.collection("gallery_items")
                .add(data)
                .addOnSuccessListener(ref -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "נוסף לגלריה ✅", Toast.LENGTH_LONG).show();
                    etTitle.setText("");
                    etImageUrl.setText("");
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "שגיאה בהוספה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}