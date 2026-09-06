package com.example.tuoicay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;

public class AppUpdateManager {
    private static final String TAG = "AppUpdateManager";
    private final Activity activity;
    private long downloadId = -1;
    private AppUpdateInfo updateInfo;
    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView tvProgressPercent;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static class AppUpdateInfo {
        public long versionCode;
        public String versionName;
        public String apkUrl;
        public String releaseNotes;
        public boolean forceUpdate;

        public AppUpdateInfo() {
            // Required for Firebase
        }
    }

    public AppUpdateManager(Activity activity) {
        this.activity = activity;
    }

    public void checkForUpdate() {
        DatabaseReference updateRef = FirebaseDatabase.getInstance().getReference("appUpdate");
        updateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    updateInfo = snapshot.getValue(AppUpdateInfo.class);
                    if (updateInfo != null) {
                        checkVersionAndShowDialog(updateInfo);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase update check failed: " + error.getMessage());
            }
        });
    }

    private void checkVersionAndShowDialog(AppUpdateInfo info) {
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            long currentVersionCode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentVersionCode = pInfo.getLongVersionCode();
            } else {
                currentVersionCode = (long) pInfo.versionCode;
            }

            if (info.versionCode > currentVersionCode) {
                showUpdateDialog(info);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void showUpdateDialog(AppUpdateInfo info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Có phiên bản mới!");
        
        StringBuilder message = new StringBuilder();
        message.append("Phiên bản hiện tại: ").append(getCurrentVersionName()).append("\n");
        message.append("Phiên bản mới: ").append(info.versionName).append("\n\n");
        if (info.releaseNotes != null && !info.releaseNotes.isEmpty()) {
            message.append("Nội dung cập nhật:\n").append(info.releaseNotes);
        }
        builder.setMessage(message.toString());

        builder.setPositiveButton("Cập nhật", (dialog, which) -> startDownload(info.apkUrl));

        if (!info.forceUpdate) {
            builder.setNegativeButton("Để sau", (dialog, which) -> dialog.dismiss());
            builder.setCancelable(true);
        } else {
            builder.setCancelable(false);
        }

        builder.show();
    }

    private String getCurrentVersionName() {
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "Unknown";
        }
    }

    private void startDownload(String url) {
        showProgressDialog();

        File file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "vuoncay_update.apk");
        if (file.exists()) {
            file.delete();
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Đang tải bản cập nhật");
        request.setDescription("Vui lòng đợi...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, "vuoncay_update.apk");

        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            downloadId = manager.enqueue(request);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
            } else {
                activity.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
            
            startProgressThread(manager);
        }
    }

    private void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Đang tải bản cập nhật...");
        builder.setCancelable(false);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        layout.addView(progressBar);

        tvProgressPercent = new TextView(activity);
        tvProgressPercent.setText("0%");
        tvProgressPercent.setPadding(0, 20, 0, 0);
        tvProgressPercent.setTextColor(Color.BLACK);
        layout.addView(tvProgressPercent);

        builder.setView(layout);
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void startProgressThread(DownloadManager manager) {
        new Thread(() -> {
            boolean downloading = true;
            while (downloading) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = manager.query(query);
                if (cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                    if (statusIndex != -1 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false;
                    }

                    if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                        int bytesDownloaded = cursor.getInt(bytesDownloadedIndex);
                        int bytesTotal = cursor.getInt(bytesTotalIndex);

                        if (bytesTotal > 0) {
                            final int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                            handler.post(() -> {
                                if (progressBar != null) progressBar.setProgress(progress);
                                if (tvProgressPercent != null) tvProgressPercent.setText(progress + "%");
                            });
                        }
                    }
                }
                cursor.close();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == id) {
                handler.post(() -> {
                    if (progressDialog != null) progressDialog.dismiss();
                    try {
                        activity.unregisterReceiver(onDownloadComplete);
                    } catch (Exception ignored) {}
                    installApk();
                });
            }
        }
    };

    private void installApk() {
        File file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "vuoncay_update.apk");
        if (!file.exists()) {
            Toast.makeText(activity, "Không thể tải bản cập nhật. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            return;
        }

        Uri apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                showInstallPermissionDialog();
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    private void showInstallPermissionDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Cần cấp quyền")
                .setMessage("Bạn cần cho phép ứng dụng cài đặt ứng dụng không xác định để cập nhật phiên bản mới.")
                .setPositiveButton("Cài đặt", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                        intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
