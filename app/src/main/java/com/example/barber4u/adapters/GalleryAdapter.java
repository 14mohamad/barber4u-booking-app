package com.example.barber4u.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.barber4u.R;
import com.example.barber4u.models.GalleryItem;

import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryVH> {

    public interface OnItemSelectedListener {
        void onSelected(GalleryItem item);
    }

    private final List<GalleryItem> items = new ArrayList<>();
    private int selectedPos = RecyclerView.NO_POSITION;
    private final OnItemSelectedListener listener;

    public GalleryAdapter(@NonNull OnItemSelectedListener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<GalleryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        selectedPos = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
        listener.onSelected(null);
    }

    public GalleryItem getSelectedOrNull() {
        if (selectedPos < 0 || selectedPos >= items.size()) return null;
        return items.get(selectedPos);
    }

    public void clearSelection() {
        int old = selectedPos;
        selectedPos = RecyclerView.NO_POSITION;
        if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
        listener.onSelected(null);
    }

    @NonNull
    @Override
    public GalleryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery, parent, false);
        return new GalleryVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryVH holder, int position) {
        GalleryItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle() == null ? "" : item.getTitle());

        Glide.with(holder.img.getContext())
                .load(item.getImageUrl())
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(holder.img);

        boolean selected = (position == selectedPos);
        holder.overlay.setVisibility(selected ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            int old = selectedPos;
            selectedPos = holder.getAdapterPosition();

            if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
            if (selectedPos != RecyclerView.NO_POSITION) notifyItemChanged(selectedPos);

            listener.onSelected(getSelectedOrNull());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class GalleryVH extends RecyclerView.ViewHolder {
        ImageView img;
        View overlay;
        TextView tvTitle;

        GalleryVH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img);
            overlay = itemView.findViewById(R.id.overlay);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
}