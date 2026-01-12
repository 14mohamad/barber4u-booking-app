package com.example.barber4u.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber4u.R;
import com.example.barber4u.models.Appointment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BarberAppointmentsAdapter
        extends RecyclerView.Adapter<BarberAppointmentsAdapter.AppointmentViewHolder> {

    private final List<Appointment> items = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void setItems(List<Appointment> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_barber, parent, false);
        return new AppointmentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appt = items.get(position);

        holder.tvCustomerEmail.setText("Customer: " + appt.getUserEmail());
        holder.tvDateTime.setText("Date: " + appt.getDate() + " " + appt.getTime());
        holder.tvBranch.setText("Branch: " + appt.getBranchName());
        holder.tvStatus.setText("Status: " + appt.getStatus());

        // כפתור אישור
        holder.btnApprove.setOnClickListener(v -> updateStatus(holder, appt, "APPROVED"));

        // כפתור ביטול
        holder.btnCancel.setOnClickListener(v -> updateStatus(holder, appt, "CANCELED"));
    }

    private void updateStatus(@NonNull AppointmentViewHolder holder,
                              @NonNull Appointment appt,
                              @NonNull String newStatus) {
        if (appt.getId() == null || appt.getId().isEmpty()) {
            Toast.makeText(holder.itemView.getContext(),
                    "Missing appointment id", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("appointments")
                .document(appt.getId())
                .update("status", newStatus)
                .addOnSuccessListener(unused -> {
                    appt.setStatus(newStatus);
                    holder.tvStatus.setText("Status: " + newStatus);
                    Toast.makeText(holder.itemView.getContext(),
                            "Status updated to " + newStatus,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(holder.itemView.getContext(),
                        "Failed to update status", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        TextView tvCustomerEmail, tvDateTime, tvBranch, tvStatus;
        MaterialButton btnApprove, btnCancel;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerEmail = itemView.findViewById(R.id.tvCustomerEmail);
            tvDateTime      = itemView.findViewById(R.id.tvDateTime);
            tvBranch        = itemView.findViewById(R.id.tvBranch);
            tvStatus        = itemView.findViewById(R.id.tvStatus);
            btnApprove      = itemView.findViewById(R.id.btnApprove);
            btnCancel       = itemView.findViewById(R.id.btnCancel);
        }
    }
}
