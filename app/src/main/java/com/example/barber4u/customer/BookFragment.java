package com.example.barber4u.customer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber4u.R;
import com.example.barber4u.adapters.GalleryAdapter;
import com.example.barber4u.customer.mvvm.BookUiState;
import com.example.barber4u.customer.mvvm.BookViewModel;
import com.example.barber4u.customer.mvvm.UiEvent;
import com.example.barber4u.models.Barber;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BookFragment extends Fragment {

    private MaterialButton btnChooseBranch;
    private android.widget.TextView tvSelectedBranch;

    private Spinner spBarber;
    private EditText etDate, etTime, etNotes;
    private android.widget.Button btnBook;
    private ProgressBar progressBook;

    private RecyclerView rvGallery;
    private GalleryAdapter galleryAdapter;

    private final List<Barber> barberList = new ArrayList<>();
    private ArrayAdapter<Barber> barberAdapter;

    private BookViewModel vm;

    public BookFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_book, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(this).get(BookViewModel.class);

        tvSelectedBranch = view.findViewById(R.id.tvSelectedBranch);
        btnChooseBranch = view.findViewById(R.id.btnChooseBranch);

        spBarber = view.findViewById(R.id.spBarber);
        etDate = view.findViewById(R.id.etDate);
        etTime = view.findViewById(R.id.etTime);
        etNotes = view.findViewById(R.id.etNotes);
        btnBook = view.findViewById(R.id.btnBook);
        progressBook = view.findViewById(R.id.progressBook);

        rvGallery = view.findViewById(R.id.rvGallery);

        // prevent manual typing
        etTime.setKeyListener(null);
        etDate.setKeyListener(null);

        setupBarberAdapter();
        setupPickers();
        setupButtons();
        setupBranchResultListener();
        observe();

        tvSelectedBranch.setText("No branch selected");
    }

    private void observe() {
        vm.state.observe(getViewLifecycleOwner(), this::render);

        vm.events.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null) return;
            UiEvent e = ev.getContentIfNotHandled();
            if (e == null) return;

            if (e.type == UiEvent.Type.TOAST) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void render(BookUiState s) {
        progressBook.setVisibility(s.loading ? View.VISIBLE : View.GONE);

        btnChooseBranch.setEnabled(!s.loading);
        spBarber.setEnabled(!s.loading && s.selectedBranchId != null);
        rvGallery.setEnabled(!s.loading);
        btnBook.setEnabled(s.canBook);

        tvSelectedBranch.setText(
                s.selectedBranchName != null ? s.selectedBranchName : "No branch selected"
        );

        barberList.clear();
        barberList.addAll(s.barbers);
        barberAdapter.notifyDataSetChanged();

        // restore spinner selection if we have selectedBarberId
        if (s.selectedBarberId != null) {
            int idx = -1;
            for (int i = 0; i < barberList.size(); i++) {
                Barber b = barberList.get(i);
                if (b != null && s.selectedBarberId.equals(b.getUid())) {
                    idx = i; break;
                }
            }
            if (idx >= 0 && spBarber.getSelectedItemPosition() != idx) {
                spBarber.setSelection(idx);
            }
        }

        if (galleryAdapter != null) {
            galleryAdapter.setItems(s.gallery);
            if (s.selectedGalleryItem == null) galleryAdapter.clearSelection();
        }

        String d = etDate.getText() == null ? "" : etDate.getText().toString();
        if (!TextUtils.equals(d, s.date)) etDate.setText(s.date);

        String t = etTime.getText() == null ? "" : etTime.getText().toString();
        if (!TextUtils.equals(t, s.time)) etTime.setText(s.time);

        String n = etNotes.getText() == null ? "" : etNotes.getText().toString();
        if (!TextUtils.equals(n, s.notes)) etNotes.setText(s.notes);
    }

    private void setupButtons() {
        btnChooseBranch.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.roleContainer, new BranchSelectionFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnBook.setOnClickListener(v -> {
            vm.onNotesChanged(etNotes.getText() == null ? "" : etNotes.getText().toString());
            vm.book();
        });
    }

    private void setupBranchResultListener() {
        getParentFragmentManager().setFragmentResultListener(
                "branch_select_result",
                this,
                (requestKey, bundle) -> {
                    String branchId = bundle.getString("branchId");
                    String branchName = bundle.getString("branchName");
                    vm.onBranchSelected(branchId, branchName);
                }
        );
    }

    private void setupBarberAdapter() {
        barberAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                barberList
        );
        barberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBarber.setAdapter(barberAdapter);

        spBarber.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= barberList.size()) return;
                vm.onBarberSelected(barberList.get(position));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupGallery() {
        rvGallery.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        galleryAdapter = new GalleryAdapter(item -> vm.onGalleryItemSelected(item));
        rvGallery.setAdapter(galleryAdapter);
        galleryAdapter.setItems(new ArrayList<>());
    }

    private void setupPickers() {
        etDate.setOnClickListener(v -> openDatePicker());
        etTime.setOnClickListener(v -> openTimePicker());
    }

    private void openDatePicker() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (datePicker, y, m, d) -> {
                    String text = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, (m + 1), y);
                    vm.onDateChanged(text);
                },
                year, month, day
        );

        Calendar minDate = Calendar.getInstance();
        minDate.set(Calendar.HOUR_OF_DAY, 0);
        minDate.set(Calendar.MINUTE, 0);
        minDate.set(Calendar.SECOND, 0);
        minDate.set(Calendar.MILLISECOND, 0);
        dialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        dialog.show();
    }

    private void openTimePicker() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (timePicker, h, m) -> {
                    String text = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                    if (!vm.isWorkHourValid(text)) {
                        Toast.makeText(requireContext(), "אפשר לבחור שעה רק בין 10:00 ל-20:00", Toast.LENGTH_LONG).show();
                        return;
                    }
                    vm.onTimeChanged(text);
                },
                hour, minute, true
        );
        dialog.show();
    }
}