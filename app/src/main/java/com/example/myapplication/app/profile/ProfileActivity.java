package com.example.myapplication.app.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvTotalTimeBig, tvGymTimeSmall, tvStudyTimeSmall, tvBedroomTimeSmall,
            tvGymTimeCard, tvStudyTimeCard, tvBedroomTimeCard;
    private ImageButton btnBack;
    private MaterialButton btnTabToday, btnTabWeek, btnTabMonth;

    private final View[] bars = new View[7];
    private final TextView[] barLabels = new TextView[7];

    private FirebaseFirestore db;
    private String uid;

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) uid = user.getUid();

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupChartViews();
        setupListeners();

        loadDailyData();
        updateTabUI(0);
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);

        tvTotalTimeBig      = findViewById(R.id.tvTotalTimeBig);
        tvGymTimeSmall      = findViewById(R.id.tvGymTimeSmall);
        tvStudyTimeSmall    = findViewById(R.id.tvStudyTimeSmall);
        tvBedroomTimeSmall  = findViewById(R.id.tvBedroomTimeSmall);
        tvGymTimeCard       = findViewById(R.id.tvGymTimeCard);
        tvStudyTimeCard     = findViewById(R.id.tvStudyTimeCard);
        tvBedroomTimeCard   = findViewById(R.id.tvBedroomTimeCard);

        btnTabToday = findViewById(R.id.btnTabToday);
        btnTabWeek  = findViewById(R.id.btnTabWeek);
        btnTabMonth = findViewById(R.id.btnTabMonth);
    }

    private void setupChartViews() {
        int[] barIds = {
                R.id.bar1, R.id.bar2, R.id.bar3, R.id.bar4,
                R.id.bar5, R.id.bar6, R.id.bar7
        };
        int[] lblIds = {
                R.id.lbl1, R.id.lbl2, R.id.lbl3, R.id.lbl4,
                R.id.lbl5, R.id.lbl6, R.id.lbl7
        };
        for (int i = 0; i < 7; i++) {
            bars[i] = findViewById(barIds[i]);
            barLabels[i] = findViewById(lblIds[i]);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnTabToday.setOnClickListener(v -> {
            updateTabUI(0);
            loadDailyData();
        });

        btnTabWeek.setOnClickListener(v -> {
            updateTabUI(1);
            loadHistoryData(7);
        });

        btnTabMonth.setOnClickListener(v -> {
            updateTabUI(2);
            loadHistoryData(30);
        });
    }

    // -------- Today --------
    private void loadDailyData() {
        if (uid == null) return;

        String todayDate = DATE_FMT.format(new Date());

        db.collection("users")
                .document(uid)
                .collection("history")
                .document(todayDate)
                .get()
                .addOnSuccessListener(doc -> {
                    long tGym = getLong(doc, "time_gym");
                    long tStudy = getLong(doc, "time_study");
                    long tBedroom = getLong(doc, "time_bedroom");
                    long tTotal = getLong(doc, "total_time");

                    updateUIValues(tTotal, tGym, tStudy, tBedroom);

                    resetChart();
                    updateSingleBar(6, tTotal, "Today");
                });
    }

    // -------- Week / Month --------
    private void loadHistoryData(int daysLimit) {
        if (uid == null) return;

        db.collection("users")
                .document(uid)
                .collection("history")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(daysLimit)
                .get()
                .addOnSuccessListener(qs -> {

                    long sumGym = 0, sumStudy = 0, sumBedroom = 0, sumTotal = 0;
                    long[] chartValues = new long[7];
                    String[] chartLabels = new String[7];

                    List<DocumentSnapshot> docs = qs.getDocuments();
                    int chartIndex = 6;

                    for (DocumentSnapshot doc : docs) {
                        long dTotal   = getLong(doc, "total_time");
                        long dGym     = getLong(doc, "time_gym");
                        long dStudy   = getLong(doc, "time_study");
                        long dBedroom = getLong(doc, "time_bedroom");

                        sumGym     += dGym;
                        sumStudy   += dStudy;
                        sumBedroom += dBedroom;
                        sumTotal   += dTotal;

                        if (chartIndex >= 0) {
                            chartValues[chartIndex] = dTotal;

                            String dateStr = doc.getString("date");
                            chartLabels[chartIndex] =
                                    (dateStr != null && dateStr.length() >= 10)
                                            ? dateStr.substring(8, 10)
                                            : "-";
                            chartIndex--;
                        }
                    }

                    updateUIValues(sumTotal, sumGym, sumStudy, sumBedroom);
                    updateChart(chartValues, chartLabels);
                });
    }

    // -------- Update Texts --------
    private void updateUIValues(long total, long gym, long study, long bedroom) {
        tvTotalTimeBig.setText(formatTime(total));

        tvGymTimeSmall.setText(formatMinutes(gym));
        tvStudyTimeSmall.setText(formatMinutes(study));
        tvBedroomTimeSmall.setText(formatMinutes(bedroom));

        tvGymTimeCard.setText(formatTime(gym));
        tvStudyTimeCard.setText(formatTime(study));
        tvBedroomTimeCard.setText(formatTime(bedroom));
    }

    // -------- Chart helpers --------
    private void updateChart(long[] values, String[] labels) {
        long max = 1;
        for (long v : values) if (v > max) max = v;

        for (int i = 0; i < 7; i++) {
            float weight = (float) values[i] / (float) max;
            if (weight < 0.05f) weight = 0.05f;

            LinearLayout.LayoutParams p =
                    (LinearLayout.LayoutParams) bars[i].getLayoutParams();
            p.weight = weight;
            bars[i].setLayoutParams(p);

            barLabels[i].setText(labels[i] != null ? labels[i] : "-");
        }
    }

    private void resetChart() {
        for (int i = 0; i < 7; i++) {
            LinearLayout.LayoutParams p =
                    (LinearLayout.LayoutParams) bars[i].getLayoutParams();
            p.weight = 0.05f;
            bars[i].setLayoutParams(p);
            barLabels[i].setText("-");
        }
    }

    private void updateSingleBar(int index, long value, String label) {
        LinearLayout.LayoutParams p =
                (LinearLayout.LayoutParams) bars[index].getLayoutParams();

        float weight = (float) value / (60 * 60_000f);
        if (weight > 1f) weight = 1f;
        if (weight < 0.05f) weight = 0.05f;

        p.weight = weight;
        bars[index].setLayoutParams(p);
        barLabels[index].setText(label);
    }

    // -------- Tabs UI --------
    private void updateTabUI(int index) {
        int activeColor   = getResources().getColor(R.color.accent_green);
        int inactiveColor = android.graphics.Color.TRANSPARENT;
        int activeText    = getResources().getColor(R.color.black);
        int inactiveText  = android.graphics.Color.parseColor("#BFCAD6");

        MaterialButton[] buttons = {btnTabToday, btnTabWeek, btnTabMonth};

        for (int i = 0; i < buttons.length; i++) {
            MaterialButton b = buttons[i];
            if (i == index) {
                b.setBackgroundColor(activeColor);
                b.setTextColor(activeText);
            } else {
                b.setBackgroundColor(inactiveColor);
                b.setTextColor(inactiveText);
            }
        }
    }

    // -------- Helpers --------
    private long getLong(DocumentSnapshot doc, String key) {
        Long v = doc.getLong(key);
        return v != null ? v : 0;
    }

    private String formatTime(long ms) {
        long totalMins = ms / 60_000;
        long h = totalMins / 60;
        long m = totalMins % 60;
        return h + "h " + m + "m";
    }

    private String formatMinutes(long ms) {
        return (ms / 60_000) + "m";
    }
}
