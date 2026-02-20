package com.example.barber4u.customer.mvvm;

import com.example.barber4u.models.Barber;
import com.example.barber4u.models.GalleryItem;

import java.util.ArrayList;
import java.util.List;

public class BookUiState {
    public boolean loading = false;

    public String selectedBranchId = null;
    public String selectedBranchName = null;

    public List<Barber> barbers = new ArrayList<>();
    public String selectedBarberId = null;

    public List<GalleryItem> gallery = new ArrayList<>();
    public GalleryItem selectedGalleryItem = null;

    public String date = "";
    public String time = "";
    public String notes = "";

    public boolean canBook = false;

    public BookUiState copy() {
        BookUiState s = new BookUiState();
        s.loading = this.loading;

        s.selectedBranchId = this.selectedBranchId;
        s.selectedBranchName = this.selectedBranchName;

        s.barbers = new ArrayList<>(this.barbers);
        s.selectedBarberId = this.selectedBarberId;

        s.gallery = new ArrayList<>(this.gallery);
        s.selectedGalleryItem = this.selectedGalleryItem;

        s.date = this.date;
        s.time = this.time;
        s.notes = this.notes;

        s.canBook = this.canBook;
        return s;
    }
}