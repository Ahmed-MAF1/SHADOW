package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvBags, tvHobbies, tvTimeGym, tvTimeStudy, tvTimeBedroom;
    private SharedPreferences prefs;
    private static final String PREFS = "my_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // اصنع layout بسيط بنفس الأسماء

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        tvName = findViewById(R.id.tvName);
        tvBags = findViewById(R.id.tvBags);
        tvHobbies = findViewById(R.id.tvHobbies);
        tvTimeGym = findViewById(R.id.tvTimeGym);
        tvTimeStudy = findViewById(R.id.tvTimeStudy);
        tvTimeBedroom = findViewById(R.id.tvTimeBedroom);

        tvName.setText("MAFGHOST");
        tvBags.setText("Backpack, Gym bag");
        tvHobbies.setText("Reading, Programming, Gym");

        long tGym = prefs.getLong("time_gym", 0L);
        long tStudy = prefs.getLong("time_study", 0L);
        long tBedroom = prefs.getLong("time_bedroom", 0L);

        tvTimeGym.setText(formatMinutes(tGym));
        tvTimeStudy.setText(formatMinutes(tStudy));
        tvTimeBedroom.setText(formatMinutes(tBedroom));
    }

    private String formatMinutes(long ms) {
        long minutes = ms / 60000;
        return minutes + " min";
    }
}
