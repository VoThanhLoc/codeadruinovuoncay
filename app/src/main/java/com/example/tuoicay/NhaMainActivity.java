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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NhaMainActivity extends AppCompatActivity {
    CardView btn_nhalive;
    CardView btn_nhatime;
    CardView btn_nhalichsu;
    CardView btn_nhawifi;
    TextView tvDeviceStatus;
    View viewStatusDot;
    TextView tvWifiSignal, tvLastUpdate;
    View layoutStatusPill;
    android.widget.ImageButton btnBack;

    private long lastSeenTimestamp = 0;
    private String firebaseStatus = "";
    private final android.os.Handler statusHandler = new android.os.Handler();
    private final Runnable statusRunnable = new Runnable() {
        @Override
        public void run() {
            updateStatusUI();
            statusHandler.postDelayed(this, 5000);
        }
    };

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
        btnBack = findViewById(R.id.btn_back_nha);

        btnBack.setOnClickListener(v -> finish());

        statusHandler.post(statusRunnable);

        // Lắng nghe trạng thái thiết bị từ Firebase
        DatabaseReference deviceRef = FirebaseDatabase.getInstance().getReference("device");
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                firebaseStatus = snapshot.child("status").getValue(String.class);
                Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);
                if (lastSeen != null) {
                    lastSeenTimestamp = lastSeen;
                }
                
                updateStatusUI();

                Object versionObj = snapshot.child("firmwareVersion").getValue();
                if (versionObj != null) {
                    tvWifiSignal.setText("v" + versionObj);
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

    private void updateStatusUI() {
        long currentTime = System.currentTimeMillis();
        boolean isTimeout = (currentTime - lastSeenTimestamp) > 30000;

        if (isTimeout || !"Online".equalsIgnoreCase(firebaseStatus)) {
            tvDeviceStatus.setText("OFFLINE");
            viewStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
            layoutStatusPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#B71C1C")));
            
            // Cập nhật Firebase nếu phát hiện timeout
            if (isTimeout && "Online".equalsIgnoreCase(firebaseStatus)) {
                FirebaseDatabase.getInstance().getReference("device/status").setValue("Offline");
            }
        } else {
            tvDeviceStatus.setText("ONLINE");
            viewStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GREEN));
            layoutStatusPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D47A1")));
        }
        tvDeviceStatus.setTextColor(Color.WHITE);
        tvLastUpdate.setText(formatLastSeen(lastSeenTimestamp));
    }

    private String formatLastSeen(long timestamp) {
        if (timestamp == 0) return "Chưa rõ";
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 60000) return "Vừa xong";
        if (diff < 3600000) return (diff / 60000) + " phút trước";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        statusHandler.removeCallbacks(statusRunnable);
    }
}
