package com.example.barber4u.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.barber4u.R;
import com.example.barber4u.data.firebase.FirebaseProvider;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AdminManageFragment extends Fragment {

    private static final String COLL_BRANCHES = "branches";
    private static final String COLL_BARBERS  = "barbers";
    private static final String COLL_USERS    = "users";

    // מסמך נעילה לסנכרון (כדי להפחית מירוצים)
    private static final String COLL_META = "meta";
    private static final String DOC_BRANCH_LOCK = "branch_lock";
    private static final String FIELD_LAST_WRITE = "lastWriteAt";

    private FirebaseFirestore db;

    private View btnAddBranch, btnAddBarber, btnChangeRole;
    private ListView listBranches, listBarbers, listUsers;

    private final List<SimpleItem> branches = new ArrayList<>();
    private final List<SimpleItem> barbers  = new ArrayList<>();
    private final List<SimpleItem> users    = new ArrayList<>();

    private ArrayAdapter<SimpleItem> branchesAdapter;
    private ArrayAdapter<SimpleItem> barbersAdapter;
    private ArrayAdapter<SimpleItem> usersAdapter;

    public AdminManageFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_manage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseProvider.db();

        btnAddBranch  = view.findViewById(R.id.btnAddBranch);
        btnAddBarber  = view.findViewById(R.id.btnAddBarber);
        btnChangeRole = view.findViewById(R.id.btnChangeRole);

        listBranches = view.findViewById(R.id.listBranches);
        listBarbers  = view.findViewById(R.id.listBarbers);
        listUsers    = view.findViewById(R.id.listUsers);

        branchesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, branches);
        barbersAdapter  = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, barbers);
        usersAdapter    = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, users);

        listBranches.setAdapter(branchesAdapter);
        listBarbers.setAdapter(barbersAdapter);
        listUsers.setAdapter(usersAdapter);

        btnAddBranch.setOnClickListener(v -> showAddBranchDialog());
        btnAddBarber.setOnClickListener(v -> showCreateOrUpdateBarberDialog());
        btnChangeRole.setOnClickListener(v -> showChangeRoleDialog());

        // Toggle active on long click
        listBranches.setOnItemLongClickListener((parent, v, position, id) -> {
            SimpleItem item = branches.get(position);
            toggleActive(COLL_BRANCHES, item.id, item.active);
            return true;
        });

        listBarbers.setOnItemLongClickListener((parent, v, position, id) -> {
            SimpleItem item = barbers.get(position);
            toggleActive(COLL_BARBERS, item.id, item.active);
            return true;
        });

        loadAll();
    }

    private void loadAll() {
        loadBranches();
        loadBarbers();
        loadUsers();
    }

    private void loadBranches() {
        branches.clear();
        branchesAdapter.notifyDataSetChanged();

        db.collection(COLL_BRANCHES)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        String id = doc.getId(); // branch1/branch2/...
                        String name = safe(doc.getString("name"));
                        boolean active = Boolean.TRUE.equals(doc.getBoolean("active"));
                        branches.add(new SimpleItem(id, id + " - " + name, active));
                    }
                    branchesAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> toast("טעינה נכשלה (branches): " + e.getMessage()));
    }

    private void loadBarbers() {
        barbers.clear();
        barbersAdapter.notifyDataSetChanged();

        db.collection(COLL_BARBERS)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        String id = doc.getId(); // uid
                        String name = safe(doc.getString("name"));
                        boolean active = Boolean.TRUE.equals(doc.getBoolean("active"));

                        @SuppressWarnings("unchecked")
                        List<String> branchIds = (List<String>) doc.get("branchIds");

                        String branchesTxt = (branchIds == null || branchIds.isEmpty())
                                ? "ללא מספרות"
                                : TextUtils.join(", ", branchIds);

                        barbers.add(new SimpleItem(id, name + " (" + branchesTxt + ")", active));
                    }
                    barbersAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> toast("טעינה נכשלה (barbers): " + e.getMessage()));
    }

    private void loadUsers() {
        users.clear();
        usersAdapter.notifyDataSetChanged();

        db.collection(COLL_USERS)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        String uid = doc.getId();
                        String email = safe(doc.getString("email"));
                        String role = safe(doc.getString("role"));
                        users.add(new SimpleItem(uid, email + " (" + role + ")", true));
                    }
                    usersAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> toast("טעינה נכשלה (users): " + e.getMessage()));
    }

    // =========================
    // ADD BRANCH (AUTO ID) + שימוש מחדש במספרים שנמחקו + GeoPoint
    // =========================

    private void showAddBranchDialog() {
        EditText etName = new EditText(requireContext());
        etName.setHint("שם המספרה (למשל: AbedBarber)");

        EditText etAddress = new EditText(requireContext());
        etAddress.setHint("כתובת/עיר (למשל: Tel Aviv)");

        EditText etPhone = new EditText(requireContext());
        etPhone.setHint("טלפון (למשל: 0533041112)");

        EditText etLat = new EditText(requireContext());
        etLat.setHint("Latitude (אופציונלי, למשל: 32.0853)");

        EditText etLng = new EditText(requireContext());
        etLng.setHint("Longitude (אופציונלי, למשל: 34.7818)");

        ViewGroup box = verticalBox();
        box.addView(etName);    box.addView(space());
        box.addView(etAddress); box.addView(space());
        box.addView(etPhone);   box.addView(space());
        box.addView(etLat);     box.addView(space());
        box.addView(etLng);

        new AlertDialog.Builder(requireContext())
                .setTitle("הוספת מספרה חדשה")
                .setView(box)
                .setPositiveButton("שמירה", (d, which) -> {
                    String name = etName.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String latStr = etLat.getText().toString().trim();
                    String lngStr = etLng.getText().toString().trim();

                    if (TextUtils.isEmpty(name)) { toast("שם המספרה חובה"); return; }
                    if (TextUtils.isEmpty(address)) { toast("כתובת/עיר חובה"); return; }

                    Double lat = parseDoubleOrNull(latStr);
                    Double lng = parseDoubleOrNull(lngStr);
                    if ((lat != null && lng == null) || (lat == null && lng != null)) {
                        toast("אם מזינים מיקום – חייבים גם Latitude וגם Longitude");
                        return;
                    }

                    // ✅ חישוב מספר פנוי ראשון + יצירה בטוחה עם retry
                    createBranchReusingNumbers(name, address, phone, lat, lng);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * 1) קוראים את כל ה-branches מחוץ ל-transaction
     * 2) מחשבים את המספר הפנוי הראשון (למשל אם branch3 נמחק -> נבחר 3)
     * 3) יוצרים בתוך transaction, ואם במקרה יש התנגשות -> ננסה את הבא
     */
    private void createBranchReusingNumbers(String name, String address, String phone, Double lat, Double lng) {
        db.collection(COLL_BRANCHES)
                .get()
                .addOnSuccessListener(snap -> {
                    HashSet<Long> used = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Long n = extractBranchNumber(doc.getId());
                        if (n != null && n > 0) used.add(n);
                    }

                    long candidate = 1;
                    while (used.contains(candidate)) candidate++;

                    attemptCreateBranchTransaction(name, address, phone, lat, lng, candidate, 30);
                })
                .addOnFailureListener(e -> toast("שמירה נכשלה (קריאת branches): " + e.getMessage()));
    }

    /**
     * מנסה ליצור branch{candidate} בתוך Transaction.
     * אם כבר קיים (מירוץ/התנגשות) -> מנסה candidate+1 עד maxRetries.
     */
    private void attemptCreateBranchTransaction(String name, String address, String phone,
                                                Double lat, Double lng,
                                                long candidate, int maxRetries) {
        if (maxRetries <= 0) {
            toast("לא הצלחנו ליצור מספרה (יותר מדי ניסיונות). נסי שוב.");
            return;
        }

        final String branchId = "branch" + candidate;

        DocumentReference lockRef = db.collection(COLL_META).document(DOC_BRANCH_LOCK);
        DocumentReference branchRef = db.collection(COLL_BRANCHES).document(branchId);

        db.runTransaction(tx -> {
            // ✅ קודם כל קריאות (READS)
            DocumentSnapshot lockSnap = tx.get(lockRef);
            DocumentSnapshot existing = tx.get(branchRef);

            if (existing.exists()) {
                throw new IllegalStateException("BRANCH_ID_TAKEN");
            }

            // ✅ עכשיו כתיבות (WRITES) - אחרי כל הקריאות
            if (!lockSnap.exists()) {
                Map<String, Object> init = new HashMap<>();
                init.put(FIELD_LAST_WRITE, Timestamp.now());
                tx.set(lockRef, init);
            } else {
                tx.update(lockRef, FIELD_LAST_WRITE, Timestamp.now());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("branchId", branchId);
            data.put("name", name);
            data.put("address", address);
            if (!TextUtils.isEmpty(phone)) data.put("phone", phone);
            data.put("active", true);
            data.put("createdAt", Timestamp.now());
            data.put("updatedAt", Timestamp.now());

            if (lat != null && lng != null) {
                data.put("location", new GeoPoint(lat, lng)); // ✅ GeoPoint
            }

            tx.set(branchRef, data);
            return branchId;

        }).addOnSuccessListener(createdId -> {
            toast("מספרה נוספה: " + createdId);
            loadBranches();
        }).addOnFailureListener(e -> {
            if (e != null && e.getMessage() != null && e.getMessage().contains("BRANCH_ID_TAKEN")) {
                attemptCreateBranchTransaction(name, address, phone, lat, lng, candidate + 1, maxRetries - 1);
            } else {
                toast("שמירה נכשלה: " + (e != null ? e.getMessage() : "שגיאה לא ידועה"));
            }
        });
    }

    private Long extractBranchNumber(String branchId) {
        if (branchId == null) return null;
        branchId = branchId.trim();
        if (!branchId.startsWith("branch")) return null;
        try {
            return Long.parseLong(branchId.substring("branch".length()));
        } catch (Exception e) {
            return null;
        }
    }

    // =========================
    // BARBER: link/update by user email + multi branches
    // =========================

    private void showCreateOrUpdateBarberDialog() {
        db.collection(COLL_BRANCHES)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(branchSnap -> {
                    List<SimpleItem> branchOptions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : branchSnap) {
                        String docId = doc.getId();
                        String name = safe(doc.getString("name"));
                        branchOptions.add(new SimpleItem(docId, docId + " - " + name, true));
                    }

                    if (branchOptions.isEmpty()) {
                        toast("אין מספרות פעילות. הוסיפי מספרה קודם.");
                        return;
                    }

                    EditText etUserEmail = new EditText(requireContext());
                    etUserEmail.setHint("אימייל של המשתמש (חייב להיות קיים ב-users)");

                    EditText etBarberName = new EditText(requireContext());
                    etBarberName.setHint("שם להצגה של הספר (למשל: Eitan)");

                    String[] labels = new String[branchOptions.size()];
                    boolean[] checked = new boolean[branchOptions.size()];
                    for (int i = 0; i < branchOptions.size(); i++) {
                        labels[i] = branchOptions.get(i).title;
                        checked[i] = false;
                    }

                    ViewGroup box = verticalBox();
                    box.addView(etUserEmail); box.addView(space());
                    box.addView(etBarberName); box.addView(space());

                    new AlertDialog.Builder(requireContext())
                            .setTitle("קישור/עדכון ספר (יכול לעבוד בכמה מספרות)")
                            .setView(box)
                            .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                            .setPositiveButton("שמירה", (d, which) -> {
                                String email = etUserEmail.getText().toString().trim();
                                String barberName = etBarberName.getText().toString().trim();

                                if (TextUtils.isEmpty(email)) { toast("אימייל חובה"); return; }
                                if (TextUtils.isEmpty(barberName)) { toast("שם ספר חובה"); return; }

                                List<String> selectedBranchIds = new ArrayList<>();
                                for (int i = 0; i < branchOptions.size(); i++) {
                                    if (checked[i]) selectedBranchIds.add(branchOptions.get(i).id);
                                }

                                if (selectedBranchIds.isEmpty()) {
                                    toast("חובה לבחור לפחות מספרה אחת");
                                    return;
                                }

                                linkBarberByEmail(email, barberName, selectedBranchIds);
                            })
                            .setNegativeButton("ביטול", null)
                            .show();
                })
                .addOnFailureListener(e -> toast("טעינת branches נכשלה: " + e.getMessage()));
    }

    private void linkBarberByEmail(String email, String barberName, List<String> branchIds) {
        db.collection(COLL_USERS)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(userSnap -> {
                    if (userSnap.isEmpty()) {
                        toast("המשתמש לא נמצא ב-users");
                        return;
                    }

                    DocumentSnapshot userDoc = userSnap.getDocuments().get(0);
                    String uid = userDoc.getId();

                    Map<String, Object> data = new HashMap<>();
                    data.put("name", barberName);
                    data.put("branchIds", branchIds);
                    data.put("active", true);
                    data.put("updatedAt", Timestamp.now());

                    db.collection(COLL_BARBERS)
                            .document(uid)
                            .set(data)
                            .addOnSuccessListener(v -> {
                                toast("הספר נשמר (id = uid)");
                                loadBarbers();
                            })
                            .addOnFailureListener(e -> toast("שמירה נכשלה: " + e.getMessage()));
                })
                .addOnFailureListener(e -> toast("חיפוש משתמש נכשל: " + e.getMessage()));
    }

    // =========================
    // CHANGE ROLE
    // =========================

    private void showChangeRoleDialog() {
        EditText etEmail = new EditText(requireContext());
        etEmail.setHint("אימייל משתמש");

        String[] roles = new String[]{"CUSTOMER", "BARBER", "ADMIN"};
        final int[] selected = {0};

        new AlertDialog.Builder(requireContext())
                .setTitle("שינוי תפקיד משתמש")
                .setView(etEmail)
                .setSingleChoiceItems(roles, 0, (dialog, which) -> selected[0] = which)
                .setPositiveButton("עדכון", (d, which) -> {
                    String email = etEmail.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) { toast("אימייל חובה"); return; }

                    String roleStr = roles[selected[0]];

                    db.collection(COLL_USERS)
                            .whereEqualTo("email", email)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(snap -> {
                                if (snap.isEmpty()) {
                                    toast("המשתמש לא נמצא ב-users");
                                    return;
                                }

                                DocumentSnapshot userDoc = snap.getDocuments().get(0);
                                String uid = userDoc.getId();

                                db.collection(COLL_USERS)
                                        .document(uid)
                                        .update("role", roleStr)
                                        .addOnSuccessListener(v -> {
                                            toast("התפקיד עודכן");
                                            loadUsers();
                                        })
                                        .addOnFailureListener(e -> toast("עדכון נכשל: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> toast("חיפוש נכשל: " + e.getMessage()));
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // =========================
    // TOGGLE ACTIVE
    // =========================

    private void toggleActive(String collection, String docId, boolean currentActive) {
        boolean newValue = !currentActive;
        db.collection(collection)
                .document(docId)
                .update("active", newValue, "updatedAt", Timestamp.now())
                .addOnSuccessListener(v -> {
                    toast(collection + ": active=" + newValue);
                    loadAll();
                })
                .addOnFailureListener(e -> toast("עדכון נכשל: " + e.getMessage()));
    }

    // =========================
    // Helpers
    // =========================

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private ViewGroup verticalBox() {
        android.widget.LinearLayout box = new android.widget.LinearLayout(requireContext());
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int p = dp(16);
        box.setPadding(p, p, p, p);
        return box;
    }

    private View space() {
        View v = new View(requireContext());
        v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)));
        return v;
    }

    private int dp(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }

    private Double parseDoubleOrNull(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try { return Double.parseDouble(s); }
        catch (Exception e) { return null; }
    }

    private static class SimpleItem {
        final String id;
        final String title;
        final boolean active;

        SimpleItem(String id, String title, boolean active) {
            this.id = id;
            this.title = title;
            this.active = active;
        }

        @NonNull
        @Override
        public String toString() {
            return title + (active ? "" : " (inactive)");
        }
    }
}