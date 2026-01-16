package com.example.barber4u.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber4u.R;
import com.example.barber4u.common.MessageItem;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter
        extends RecyclerView.Adapter<MessagesAdapter.VH> {

    public interface Listener {
        void onRateNow(@NonNull MessageItem item);
        void onDismiss(@NonNull MessageItem item);
    }

    private final List<MessageItem> items = new ArrayList<>();
    private final Listener listener;

    public MessagesAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<MessageItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MessageItem item = items.get(position);

        h.tvMessageText.setText(item.text);

        h.btnRateNow.setOnClickListener(v -> listener.onRateNow(item));
        h.btnDismiss.setOnClickListener(v -> listener.onDismiss(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMessageText;
        MaterialButton btnRateNow, btnDismiss;

        VH(@NonNull View v) {
            super(v);
            tvMessageText = v.findViewById(R.id.tvMessageText);
            btnRateNow = v.findViewById(R.id.btnRateNow);
            btnDismiss = v.findViewById(R.id.btnDismiss);
        }
    }
}
