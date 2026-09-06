package com.example.tuoicay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HomeMainActivity extends AppCompatActivity {
    CardView btn_garden;
    CardView btn_house;
    private AppUpdateManager updateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_main);
        btn_garden = findViewById(R.id.btn_garden);
        btn_house = findViewById(R.id.btn_house);

        // Khởi tạo và kiểm tra cập nhật
        updateManager = new AppUpdateManager(this);
        updateManager.checkForUpdate();

        btn_garden.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeMainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        btn_house.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeMainActivity.this, NhaMainActivity.class);
                startActivity(intent);
            }
        });
    }

}
