package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class RoomStatusActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_status);


        String room = getIntent().getStringExtra("room");
        if (room == null) room = "gym";

        TextView tv = findViewById(R.id.tvRoomName);
        tv.setText("Room: " + room.toUpperCase());
    }
}
