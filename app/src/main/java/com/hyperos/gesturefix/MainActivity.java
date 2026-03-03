package com.hyperos.gesturefix;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvAudit;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastAuditReport = ""; 
    
    public boolean isModuleActive() { return false; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvAudit = findViewById(R.id.tvLogs); 
        Button btnFix = findViewById(R.id.btnFix);
        Button btnExport = findViewById(R.id.btnExport);

        updateLSPosedStatus(); // Method now defined below

        if (btnFix != null) {
            btnFix.setOnClickListener(v -> {
                runRootElevatedFix();
                handler.postDelayed(this::runSystemAudit, 3000);
            });
        }

        if (btnExport != null) {
            btnExport.setOnClickListener(v -> exportAuditLog());
        }
        
        runSystemAudit();
    }

    // Logic to update the UI based on Xposed module state
    private void updateLSPosedStatus() {
        if (tvStatus != null) {
            if (isModuleActive()) {
                tvStatus.setText("LSPosed: ACTIVE");
                tvStatus.setTextColor(Color.GREEN);
            } else {
                tvStatus.setText("LSPosed: INACTIVE");
                tvStatus.setTextColor(Color.RED);
            }
        }
    }

    private void runSystemAudit() {
        StringBuilder report = new StringBuilder();
        report.append("<b>--- HYPEROS GESTURE AUDIT ---</b><br><br>");

        boolean hasOverlay = checkCommandOutput("cmd overlay list | grep gestural")
                .contains("com.android.internal.systemui.navbar.gestural");
        report.append(formatAuditLine("AOSP Gesture Overlay Found", hasOverlay));

        String navMode = checkCommandOutput("settings get secure navigation_mode");
        report.append(formatAuditLine("Secure Nav Mode (2=Gestures)", navMode.trim().equals("2")));

        String fsgFlag = checkCommandOutput("settings get global force_fsg_nav_bar");
        report.append(formatAuditLine("Global FSG Flag (1=ON)", fsgFlag.trim().equals("1")));

        report.append(formatAuditLine("LSPosed Injection Status", isModuleActive()));

        String fixedMode = checkCommandOutput("settings get secure sw_fs_gesture_fixed_mode");
        report.append(formatAuditLine("Gesture Fixed Mode (1=ON)", fixedMode.trim().equals("1")));

        lastAuditReport = report.toString();
        if (tvAudit != null) {
            tvAudit.setText(Html.fromHtml(lastAuditReport, Html.FROM_HTML_MODE_COMPACT));
        }
    }

    private void exportAuditLog() {
        if (lastAuditReport.isEmpty()) {
            Toast.makeText(this, "Run Audit first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Improved HTML to Plain Text conversion
        String plainText = lastAuditReport.replaceAll("(?i)<br/?>", "\n")
                .replaceAll("<[^>]*>", "");

        try {
            File cachePath = new File(getCacheDir(), "logs");
            cachePath.mkdirs();
            File logFile = new File(cachePath, "gesture_audit_log.txt");
            FileOutputStream stream = new FileOutputStream(logFile);
            stream.write(plainText.getBytes());
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", logFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Audit Log"));

        } catch (Exception e) {
            Toast.makeText(this, "Export Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String formatAuditLine(String title, boolean passed) {
        String color = passed ? "#00FF00" : "#FF0000";
        String status = passed ? "[PASS] " : "[FAIL] ";
        return "<font color=\"" + color + "\">" + status + title + "</font><br>";
    }

    private String checkCommandOutput(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            return line != null ? line : "null";
        } catch (Exception e) {
            return "error";
        }
    }

    private void runRootElevatedFix() {
        new Thread(() -> {
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());

                os.writeBytes("settings put secure navigation_mode 2\n");
                os.writeBytes("settings put global force_fsg_nav_bar 1\n");
                os.writeBytes("settings put secure sw_fs_gesture_fixed_mode 1\n");
                os.writeBytes("settings put secure sw_fs_gesture_navigation_mode 1\n");
                
                os.writeBytes("cmd overlay enable com.android.internal.systemui.navbar.gestural\n");
                os.writeBytes("cmd overlay disable com.android.internal.systemui.navbar.threebutton\n");

                os.writeBytes("pkill -f com.android.systemui\n");
                
                os.writeBytes("exit\n");
                os.flush();
                p.waitFor();
                
            } catch (Exception e) {
                handler.post(() -> {
                    if (tvAudit != null) tvAudit.append("\nRoot Error: " + e.getMessage());
                });
            }
        }).start();
    }
}