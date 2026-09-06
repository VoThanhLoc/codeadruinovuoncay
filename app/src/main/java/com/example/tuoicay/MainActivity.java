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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    CardView btn_tuoilive;
    CardView btn_tuoitime;
    CardView btn_lichsu;
    CardView btn_wifi;
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
            statusHandler.postDelayed(this, 5000); // Kiểm tra mỗi 5 giây
        }
    };

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
        btnBack = findViewById(R.id.btn_back_home);

        btnBack.setOnClickListener(v -> finish());

        // Bắt đầu vòng lặp kiểm tra trạng thái
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

                // Hiển thị phiên bản firmware nếu có
                Object versionObj = snapshot.child("firmwareVersion").getValue();
                if (versionObj != null) {
                    tvWifiSignal.setText("v" + versionObj.toString());
                }

                // Đọc thêm tín hiệu wifi nếu có trong Firebase
                Long rssi = snapshot.child("rssi").getValue(Long.class);
                if (rssi != null && rssi != 0) {
                    // tvWifiSignal.setText(rssi + " dBm"); // Có thể ưu tiên firmware version
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvDeviceStatus.setText("LỖI");
                tvDeviceStatus.setTextColor(Color.WHITE);
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

    private void updateStatusUI() {
        long currentTime = System.currentTimeMillis();
        // Nếu chênh lệch > 30s thì coi như Offline (30000ms)
        boolean isTimeout = (currentTime - lastSeenTimestamp) > 30000;

        if (isTimeout || !"Online".equalsIgnoreCase(firebaseStatus)) {
            tvDeviceStatus.setText("OFFLINE");
            viewStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
            layoutStatusPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#B71C1C")));
            
            // Nếu phát hiện timeout mà trên Firebase vẫn là Online thì cập nhật lại Firebase
            if (isTimeout && "Online".equalsIgnoreCase(firebaseStatus)) {
                FirebaseDatabase.getInstance().getReference("device/status").setValue("Offline");
            }
        } else {
            tvDeviceStatus.setText("ONLINE");
            viewStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GREEN));
            layoutStatusPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1B5E20")));
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
