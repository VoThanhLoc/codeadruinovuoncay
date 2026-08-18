package com.example.tuoicay;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NhaMainActivity extends AppCompatActivity {
    CardView btn_nhalive;
    CardView btn_nhatime;
    CardView btn_nhalichsu;
    CardView btn_nhawifi;
    TextView tvDeviceStatus;
    View viewStatusDot;
    TextView tvWifiSignal, tvLastUpdate;
    View layoutStatusPill;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nha);
        btn_nhalive = findViewById(R.id.btn_nhalive);
        btn_nhatime = findViewById(R.id.btn_nhatime);
        btn_nhalichsu = findViewById(R.id.btn_nhalichsu);
        btn_nhawifi = findViewById(R.id.btn_nhawifi);
        tvDeviceStatus = findViewById(R.id.tv_device_status_pill_nha);
        viewStatusDot = findViewById(R.id.view_status_dot_nha);
        tvWifiSignal = findViewById(R.id.tv_wifi_signal_nha);
        tvLastUpdate = findViewById(R.id.tv_last_update_nha);
        layoutStatusPill = findViewById(R.id.layout_status_pill_nha);

        // Lắng nghe trạng thái thiết bị từ Firebase
        DatabaseReference deviceRef = FirebaseDatabase.getInstance().getReference("device");
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.child("status").getValue(String.class);
                if (status != null) {
                    tvDeviceStatus.setText(status.toUpperCase());
                    if (status.equalsIgnoreCase("Online")) {
                        viewStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GREEN));
                        layoutStatusPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D47A1")));
                    } else {
                        viewStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
                        layoutStatusPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#B71C1C")));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        btn_nhalive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NhaMainActivity.this, NhaLiveActivity.class);
                startActivity(intent);
            }
        });

        btn_nhatime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NhaMainActivity.this, NhaTimeActivity.class);
                startActivity(intent);
            }
        });

        btn_nhalichsu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NhaMainActivity.this, NhaHistoryActivity.class);
                startActivity(intent);
            }
        });

        btn_nhawifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NhaMainActivity.this, NhaConnectActivity.class);
                startActivity(intent);
            }
        });
    }
}
