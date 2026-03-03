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
    
    /**
     * This method is hooked by the LSPosed module. 
     * In the hooked state, it returns true to indicate the module is active.
     */
    public boolean isModuleActive() { 
        return false; 
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvLogs = findViewById(R.id.tvLogs);
        Button btnFix = findViewById(R.id.btnFix);

        // Update UI status based on LSPosed injection
        if (tvStatus != null) {
            if (isModuleActive()) {
                tvStatus.setText("LSPosed: ACTIVE");
                tvStatus.setTextColor(Color.GREEN);
            } else {
                tvStatus.setText("LSPosed: INACTIVE");
                tvStatus.setTextColor(Color.RED);
            }
        }

        // Initialize the Brute Force Root Fix button
        if (btnFix != null) {
            btnFix.setOnClickListener(v -> runRootElevatedFix());
        }
        
        // Start monitoring logs for HGS_LOG tags
        startLogStream();
    }

    /**
     * Executes elevated root commands to force AOSP gesture navigation.
     * This bypasses HyperOS launcher-specific locks by enabling system-level overlays.
     */
    private void runRootElevatedFix() {
        new Thread(() -> {
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());

                // 1. Force Global Navigation Mode to Gestures (2)
                os.writeBytes("settings put secure navigation_mode 2\n");
                
                // 2. Set Xiaomi-specific gesture flags to True
                os.writeBytes("settings put global force_fsg_nav_bar 1\n");
                
                // 3. Force AOSP fixed gesture modes
                os.writeBytes("settings put secure sw_fs_gesture_fixed_mode 1\n");
                os.writeBytes("settings put secure sw_fs_gesture_navigation_mode 1\n");

                // 4. Enable the AOSP Gestural Overlay and Disable Buttons
                os.writeBytes("cmd overlay enable com.android.internal.systemui.navbar.gestural\n");
                os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.threebutton\n");

                // 5. Restart SystemUI to bind the new settings
                os.writeBytes("pkill -f com.android.systemui\n");
                
                // Cleanup and wait for process completion
                os.writeBytes("exit\n");
                os.flush();
                p.waitFor();
                
                runOnUiThread(() -> {
                    if (tvLogs != null) {
                        tvLogs.append("\n[ROOT] AOSP Gestures forced via overlay. SystemUI restarted.");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (tvLogs != null) {
                        tvLogs.append("\n[ROOT ERROR] " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    /**
     * Periodically reads system logs filtered by the HGS_LOG tag to show
     * real-time feedback from the Xposed hooks.
     */
    private void startLogStream() {
        new Thread(() -> {
            try {
                // Fetch current logs specifically for our tag and suppress others
                Process process = Runtime.getRuntime().exec("logcat -d HGS_LOG:V *:S");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                
                String logs = sb.toString();
                
                handler.post(() -> {
                    if (tvLogs != null) {
                        tvLogs.setText(logs.isEmpty() ? "Waiting for system events..." : logs);
                    }
                });
            } catch (Exception ignored) {
                // Fail silently in the background
            }

            // Schedule the next log fetch
            handler.postDelayed(this::startLogStream, 2500);
        }).start();
    }
}