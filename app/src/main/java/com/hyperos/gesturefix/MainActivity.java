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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvAudit;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastAuditReport = ""; 
    private Process logcatProcess;
private boolean isMonitoring = false;

    public boolean isModuleActive() { 
        return false; 
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvAudit = findViewById(R.id.tvLogs); 
        Button btnFix = findViewById(R.id.btnFix);
        Button btnExport = findViewById(R.id.btnExport);
        Button btnClear = findViewById(R.id.btnClear);
        
        startLogcatMonitor();
        updateLSPosedStatus();

        if (btnFix != null) {
            btnFix.setOnClickListener(v -> {
                new Thread(() -> {
                    ShellUtils.applyRootFix();
                    handler.post(() -> Toast.makeText(MainActivity.this, "Fix Applied. Rebooting UI...", Toast.LENGTH_SHORT).show());
                    handler.postDelayed(this::runSystemAudit, 4500);
                }).start();
            });
        }

if (btnClear != null) {
    btnClear.setOnClickListener(v -> {
        if (tvAudit != null) {
            tvAudit.setText("--- Logs Cleared ---\n");
            // Reset scroll position to top
            tvAudit.scrollTo(0, 0);
        }
        // Also clear the audit string so Export doesn't send old data
        lastAuditReport = "";
        
        // Optional: Re-run a fresh audit immediately after clearing
        runSystemAudit();
        
        Toast.makeText(this, "Logs refreshed", Toast.LENGTH_SHORT).show();
    });
}

        if (btnExport != null) {
            btnExport.setOnClickListener(v -> exportAuditLog());
        }
        
        runSystemAudit();
    }

    private void startLogcatMonitor() {
    if (isMonitoring) return;
    isMonitoring = true;

    new Thread(() -> {
        try {
            // We use 'su -c' so we can see system-level errors like 'Permission Denied'
            // We filter for your tag and general system errors
            logcatProcess = Runtime.getRuntime().exec(new String[]{"su", "-c", "logcat HGS_LOG:D *:E"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
            String line;

            while (isMonitoring && (line = reader.readLine()) != null) {
        final String logLine = line;
        handler.post(() -> {
            if (tvAudit != null) {
                // If the user just cleared it, this will start fresh at the bottom
                tvAudit.append(logLine + "\n");
                
                // Keep the last 1000 lines for a better history buffer
                if (tvAudit.getLineCount() > 1000) {
                    String currentText = tvAudit.getText().toString();
                    int firstNewline = currentText.indexOf('\n', currentText.length() / 2);
                    if (firstNewline != -1) {
                        tvAudit.setText("... truncated ...\n" + currentText.substring(firstNewline + 1));
                    }
                }
            }
        });
    }
}catch (Exception e) {
            handler.post(() -> tvAudit.append("Logcat Error: " + e.getMessage() + "\n"));
        }
    }).start();
}

@Override
protected void onDestroy() {
    super.onDestroy();
    isMonitoring = false;
    if (logcatProcess != null) {
        logcatProcess.destroy();
    }
}

    private void updateLSPosedStatus() {
        if (tvStatus != null) {
            boolean active = isModuleActive();
            tvStatus.setText("LSPosed: " + (active ? "ACTIVE" : "INACTIVE"));
            tvStatus.setTextColor(active ? Color.GREEN : Color.RED);
        }
    }

    private void runSystemAudit() {
        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("<b>--- HYPEROS GESTURE AUDIT ---</b><br><br>");

            // Using 'su -c' for audit ensures we don't get 'null' on HyperOS
            String overlayRaw = checkCommandOutput("cmd overlay list | grep gestural");
            boolean isEnabled = overlayRaw.contains("[x]");
            report.append(formatAuditLine("AOSP Overlay (Enabled)", isEnabled));

            String navMode = checkCommandOutput("settings get secure navigation_mode").trim();
            report.append(formatAuditLine("Nav Mode (Gesture=2): " + navMode, navMode.equals("2")));

            String fsgFlag = checkCommandOutput("settings get global force_fsg_nav_bar").trim();
            report.append(formatAuditLine("FSG Flag (ON=1): " + fsgFlag, fsgFlag.equals("1")));

            String miuiFsg = checkCommandOutput("settings get secure miui_fsg_gesture_status").trim();
            report.append(formatAuditLine("MIUI FSG Status: " + miuiFsg, miuiFsg.equals("1")));

            report.append(formatAuditLine("LSPosed Injection Status", isModuleActive()));

            lastAuditReport = report.toString();
            handler.post(() -> {
                if (tvAudit != null) {
                    tvAudit.setText(Html.fromHtml(lastAuditReport, Html.FROM_HTML_MODE_COMPACT));
                }
                updateLSPosedStatus();
            });
        }).start();
    }

    private String formatAuditLine(String title, boolean passed) {
        String color = passed ? "#00FF00" : "#FF0000";
        String status = passed ? "[PASS] " : "[FAIL] ";
        return "<font color=\"" + color + "\">" + status + title + "</font><br>";
    }

    private String checkCommandOutput(String cmd) {
        try {
            // Audit must run as root to read secure settings
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            return (line != null) ? line : "null";
        } catch (Exception e) { 
            return "error"; 
        }
    }

    private void exportAuditLog() {
        if (lastAuditReport.isEmpty()) {
            Toast.makeText(this, "Run Audit first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String plainText = lastAuditReport.replaceAll("<[^>]*>", "")
                                         .replace("[PASS]", "PASS:")
                                         .replace("[FAIL]", "FAIL:");
        try {
            File cachePath = new File(getCacheDir(), "logs");
            if (!cachePath.exists()) cachePath.mkdirs();
            
            File logFile = new File(cachePath, "gesture_audit.txt");
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
            Toast.makeText(this, "Export Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}