package com.example.tuoicay;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ConnectActivity extends AppCompatActivity {

    private TextView tvSSID, tvPassword;
    private MaterialButton btnCheckUpdate;
    private ImageButton btnBack;
    private AppUpdateManager updateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        tvSSID = findViewById(R.id.tvSSID);
        tvPassword = findViewById(R.id.tvPassword);
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate);
        btnBack = findViewById(R.id.btnBack);

        updateManager = new AppUpdateManager(this);

        btnBack.setOnClickListener(v -> finish());

        // Lấy thông tin wifi từ Firebase
        DatabaseReference wifiRef = FirebaseDatabase.getInstance().getReference("wifi");
        wifiRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String ssid = snapshot.child("ssid").getValue(String.class);
                    String password = snapshot.child("password").getValue(String.class);
                    
                    if (ssid != null) tvSSID.setText(ssid);
                    if (password != null) tvPassword.setText(password);
                } else {
                    tvSSID.setText("Chưa cấu hình");
                    tvPassword.setText("Chưa cấu hình");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ConnectActivity.this, "Lỗi tải thông tin Wifi", Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý nút kiểm tra cập nhật thủ công
        btnCheckUpdate.setOnClickListener(v -> {
            Toast.makeText(this, "Đang kiểm tra phiên bản mới...", Toast.LENGTH_SHORT).show();
            updateManager.checkForUpdate();
        });
    }
}
