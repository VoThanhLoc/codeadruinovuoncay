package com.example.tuoicay;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tuoicay.R;
import com.example.tuoicay.ScheduleAdapter;
import com.example.tuoicay.ScheduleModel;
import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
public class TimeActivity extends AppCompatActivity {
    private FlexboxLayout zoneContainer, dayContainer;
    private TimePicker timePicker;
    private EditText etDuration;
    private Button btnAddSchedule;
    private RecyclerView scheduleRecyclerView;
    private ScheduleAdapter adapter;
    private List<ScheduleModel> scheduleList = new ArrayList<>();
    private DatabaseReference databaseRef;
    private ImageButton btn_Back;
    private String[] vanOptions = {"Van1", "Van2", "Van3", "Van4", "Van5", "Van6", "Van7"};
    private String[] dayOptions = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time);

        zoneContainer = findViewById(R.id.zoneContainer);
        dayContainer = findViewById(R.id.dayContainer);
        timePicker = findViewById(R.id.timePicker);
        etDuration = findViewById(R.id.etDuration);
        btnAddSchedule = findViewById(R.id.btnAddSchedule);
        scheduleRecyclerView = findViewById(R.id.scheduleRecyclerView);

        databaseRef = FirebaseDatabase.getInstance().getReference("schedule");

        setupCheckBoxes();
        setupRecyclerView();

        btnAddSchedule.setOnClickListener(v -> addSchedule());

        loadSchedulesFromFirebase();

        btn_Back = findViewById(R.id.btn_Back);

        btn_Back.setOnClickListener(view -> finish());
    }

    private void setupCheckBoxes() {
        for (String van : vanOptions) {
            CheckBox cb = new CheckBox(this);
            cb.setText(van);
            zoneContainer.addView(cb);
        }

        for (String day : dayOptions) {
            CheckBox cb = new CheckBox(this);
            cb.setText(day);
            dayContainer.addView(cb);
        }
    }

    private void setupRecyclerView() {
        adapter = new ScheduleAdapter(scheduleList, databaseRef);
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        scheduleRecyclerView.setAdapter(adapter);
    }

    private void addSchedule() {
        List<String> selectedVans = new ArrayList<>();
        for (int i = 0; i < zoneContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) zoneContainer.getChildAt(i);
            if (cb.isChecked()) selectedVans.add(cb.getText().toString());
        }

        List<String> selectedDays = new ArrayList<>();
        for (int i = 0; i < dayContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) dayContainer.getChildAt(i);
            if (cb.isChecked()) selectedDays.add(cb.getText().toString());
        }

        String hour = String.format(Locale.getDefault(), "%02d:%02d",
                timePicker.getHour(), timePicker.getMinute());

        String duration = etDuration.getText().toString().trim();

        // ✅ VALIDATE
        if (selectedVans.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một van tưới", Toast.LENGTH_SHORT).show();
            return;
        }

        if (duration.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập thời gian tưới (phút)", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int dur = Integer.parseInt(duration);
            if (dur <= 0) {
                Toast.makeText(this, "Thời gian tưới phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Thời gian tưới phải là số hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một ngày lặp lại", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = databaseRef.push().getKey();
        ScheduleModel schedule = new ScheduleModel(id, selectedVans, hour, duration, selectedDays, "off");
        databaseRef.child(id).setValue(schedule)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã thêm lịch tưới", Toast.LENGTH_SHORT).show();
                    addHistory(schedule, "added"); // << Ghi vào lịch sử
                    resetFields(); // Xóa chọn sau khi thêm
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi thêm lịch tưới", Toast.LENGTH_SHORT).show();
                });
    }
    private void resetFields() {
        // Bỏ chọn tất cả van
        for (int i = 0; i < zoneContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) zoneContainer.getChildAt(i);
            cb.setChecked(false);
        }

        // Bỏ chọn các ngày
        for (int i = 0; i < dayContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) dayContainer.getChildAt(i);
            cb.setChecked(false);
        }

        // Đặt lại giờ hiện tại
        Calendar now = Calendar.getInstance();
        timePicker.setHour(now.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(now.get(Calendar.MINUTE));

        // Xóa nội dung thời lượng
        etDuration.setText("");
    }

    private void addHistory(ScheduleModel schedule, String status) {
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("history");
        String historyId = historyRef.push().getKey();

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("zones", schedule.getZone());
        historyData.put("startTime", schedule.getStartTime());
        historyData.put("duration", schedule.getDuration());
        historyData.put("repeatDays", schedule.getRepeatDays());
        historyData.put("status", status);
        historyData.put("type", "Hẹn Giờ");

        // Thêm timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        historyData.put("timestamp", timestamp);

        // Ghi vào Firebase
        historyRef.child(historyId).setValue(historyData);
    }

    private void loadSchedulesFromFirebase() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                scheduleList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ScheduleModel model = child.getValue(ScheduleModel.class);
                    scheduleList.add(model);
                }
                adapter.notifyDataSetChanged();
            }

            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}