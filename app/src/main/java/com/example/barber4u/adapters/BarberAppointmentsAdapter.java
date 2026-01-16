package com.example.barber4u.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber4u.R;
import com.example.barber4u.models.Appointment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class BarberAppointmentsAdapter
        extends RecyclerView.Adapter<BarberAppointmentsAdapter.AppointmentViewHolder> {

    public interface Listener {
        void onApprove(@NonNull Appointment appt);
        void onCancel(@NonNull Appointment appt);
        void onDone(@NonNull Appointment appt);
    }

    private final List<Appointment> items = new ArrayList<>();
    private final Listener listener;

    public BarberAppointmentsAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<Appointment> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /**
     * Optional: remove immediately from UI (useful when marking DONE)
     */
    public void removeById(@NonNull String appointmentId) {
        for (int i = 0; i < items.size(); i++) {
            Appointment a = items.get(i);
            if (appointmentId.equals(a.getId())) {
                items.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
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

        holder.tvCustomerEmail.setText("Customer: " + safe(appt.getUserEmail()));
        holder.tvDateTime.setText("Date: " + safe(appt.getDate()) + " " + safe(appt.getTime()));
        holder.tvBranch.setText("Branch: " + safe(appt.getBranchName()));
        holder.tvStatus.setText("Status: " + safe(appt.getStatus()));

        String status = appt.getStatus() == null ? "" : appt.getStatus();

        // Default: hide all action buttons, then show as needed
        holder.btnApprove.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);
        holder.btnDone.setVisibility(View.GONE);

        if (status.equalsIgnoreCase("PENDING")) {
            // PENDING: Approve + Cancel
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnCancel.setVisibility(View.VISIBLE);

            holder.btnApprove.setText("Approve");
            holder.btnCancel.setText("Cancel");

            holder.btnApprove.setOnClickListener(v -> listener.onApprove(appt));
            holder.btnCancel.setOnClickListener(v -> listener.onCancel(appt));

        } else if (status.equalsIgnoreCase("APPOINTMENT_SCHEDULED")) {
            // Scheduled: Done (+ optional Cancel)
            holder.btnDone.setVisibility(View.VISIBLE);
            holder.btnDone.setText("Done");

            holder.btnDone.setOnClickListener(v -> listener.onDone(appt));

            // If you also want Cancel here, uncomment:
            // holder.btnCancel.setVisibility(View.VISIBLE);
            // holder.btnCancel.setText("Cancel");
            // holder.btnCancel.setOnClickListener(v -> listener.onCancel(appt));

        } else {
            // Any other status: no buttons
            // (e.g., CANCELLED / DONE should not usually be shown)
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        TextView tvCustomerEmail, tvDateTime, tvBranch, tvStatus;
        MaterialButton btnApprove, btnCancel, btnDone;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerEmail = itemView.findViewById(R.id.tvCustomerEmail);
            tvDateTime      = itemView.findViewById(R.id.tvDateTime);
            tvBranch        = itemView.findViewById(R.id.tvBranch);
            tvStatus        = itemView.findViewById(R.id.tvStatus);

            btnApprove      = itemView.findViewById(R.id.btnApprove);
            btnCancel       = itemView.findViewById(R.id.btnCancel);
            btnDone         = itemView.findViewById(R.id.btnDone); // ✅ new button
        }
    }
}
