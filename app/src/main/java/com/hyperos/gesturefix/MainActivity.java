package com.hyperos.gesturefix;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvLogs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    public boolean isModuleActive() { return false; } // Hooked by Xposed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvLogs = findViewById(R.id.tvLogs);
        Button btnFix = findViewById(R.id.btnFix);

        if (isModuleActive()) {
            tvStatus.setText("LSPosed: ACTIVE");
            tvStatus.setTextColor(Color.GREEN);
        } else {
            tvStatus.setText("LSPosed: INACTIVE");
            tvStatus.setTextColor(Color.RED);
        }

        btnFix.setOnClickListener(v -> runRootElevatedFix());
        startLogStream();
    }

    private void runRootElevatedFix() {
        new Thread(() -> {
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());

                // Force the Gesture Flag and the Hide Gesture Line Flag
                os.writeBytes("settings put global force_fsg_nav_bar 1\n");
                os.writeBytes("settings put global hide_gesture_line 1\n");
                
                // Enable the Android Gestural Overlay
                os.writeBytes("cmd overlay enable com.android.internal.systemui.navbar.gestural\n");

                // Kill SystemUI to force it to reload the new database values
                os.writeBytes("pkill -f com.android.systemui\n");
                
                os.writeBytes("exit\n");
                os.flush();
                os.waitFor();
                runOnUiThread(() -> tvLogs.append("\n[ROOT] Applied changes and restarted SystemUI."));
            } catch (Exception e) {
                runOnUiThread(() -> tvLogs.append("\n[ROOT ERROR] " + e.getMessage()));
            }
        }).start();
    }

    private void startLogStream() {
        new Thread(() -> {
            try {
                // Fetch logs with our specific HGS_LOG tag
                Process process = Runtime.getRuntime().exec("logcat -d HGS_LOG:V *:S");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                String logs = sb.toString();
                handler.post(() -> tvLogs.setText(logs.isEmpty() ? "Waiting for system events..." : logs));
            } catch (Exception ignored) {}
            handler.postDelayed(this::startLogStream, 2500);
        }).start();
    }
}