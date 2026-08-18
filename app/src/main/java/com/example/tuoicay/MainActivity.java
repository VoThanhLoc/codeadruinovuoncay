package com.example.tuoicay;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    CardView btn_tuoilive;
    CardView btn_tuoitime;
    CardView btn_lichsu;
    CardView btn_wifi;
    TextView tvDeviceStatus;
    View viewStatusDot;
    TextView tvWifiSignal, tvLastUpdate;
    View layoutStatusPill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        btn_tuoilive = findViewById(R.id.btn_tuoilive);
        btn_tuoitime = findViewById(R.id.btn_tuoitime);
        btn_lichsu = findViewById(R.id.btn_lichsu);
        btn_wifi = findViewById(R.id.btn_wifi);
        tvDeviceStatus = findViewById(R.id.tv_device_status_pill);
        viewStatusDot = findViewById(R.id.view_status_dot);
        tvWifiSignal = findViewById(R.id.tv_wifi_signal);
        tvLastUpdate = findViewById(R.id.tv_last_update);
        layoutStatusPill = findViewById(R.id.layout_status_pill);

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
                        layoutStatusPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1B5E20")));
                    } else {
                        viewStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
                        layoutStatusPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#B71C1C")));
                    }
                } else {
                    tvDeviceStatus.setText("OFFLINE");
                    viewStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
                    layoutStatusPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#B71C1C")));
                }

                // Đọc thêm tín hiệu wifi nếu có trong Firebase
                Long rssi = snapshot.child("rssi").getValue(Long.class);
                if (rssi != null) {
                    tvWifiSignal.setText(rssi + " dBm");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvDeviceStatus.setText("Trạng thái: Lỗi kết nối");
                tvDeviceStatus.setTextColor(Color.RED);
                viewStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
            }
        });

        //xử lý sự kiện khi nhấn icon

        btn_tuoilive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(MainActivity.this, LiveActivity.class);
                startActivity(intent);
            }
        });

        btn_tuoitime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(MainActivity.this, TimeActivity.class);
                startActivity(intent);
            }
        });

        btn_lichsu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        btn_wifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ConnectActivity.class);
                startActivity(intent);
            }
        });
    }
    private void toastMessage(String message)
    {
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }
}
