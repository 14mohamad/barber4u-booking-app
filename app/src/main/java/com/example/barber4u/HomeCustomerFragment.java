package com.example.barber4u;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeCustomerFragment extends Fragment {

    private TextView tvGreeting, tvSubTitle;
    private TextView tvStatTotal, tvStatUpcoming, tvStatCanceled;
    private TextView tvNextBarber, tvNextDate, tvNextStatus;

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
        // חשוב: זה ה־XML הנכון
        return inflater.inflate(R.layout.fragment_home_customer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // חיבור ל־Views מה־XML
        tvGreeting      = view.findViewById(R.id.tvGreeting);
        tvSubTitle      = view.findViewById(R.id.tvSubTitle);
        tvStatTotal     = view.findViewById(R.id.tvStatTotal);
        tvStatUpcoming  = view.findViewById(R.id.tvStatUpcoming);
        tvStatCanceled  = view.findViewById(R.id.tvStatCanceled);
        tvNextBarber    = view.findViewById(R.id.tvNextBarber);
        tvNextDate      = view.findViewById(R.id.tvNextDate);
        tvNextStatus    = view.findViewById(R.id.tvNextStatus);

        // שם המשתמש שהעברנו מה־Login (אם יש)
        String name = null;
        Bundle args = getArguments();
        if (args != null) {
            name = args.getString("userName");
        }
        if (name == null || name.trim().isEmpty()) {
            name = "Customer";
        }

        tvGreeting.setText("Hi, " + name);
        tvSubTitle.setText("Welcome back 👋");

        // כרגע נתונים דמיוניים – רק כדי שתראי עיצוב
        tvStatTotal.setText("12");
        tvStatUpcoming.setText("2");
        tvStatCanceled.setText("1");

        tvNextBarber.setText("Barber: Ahmed");
        tvNextDate.setText("Date: 15/12/2025 - 18:30");
        tvNextStatus.setText("Status: Pending");
    }
}
