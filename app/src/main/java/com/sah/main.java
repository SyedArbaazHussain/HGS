package com.sah;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

/**
 * Main management interface for HyperOS Gesture Fix.
 * Refactored for modern libxposed API and SDK 35.
 */
public class main extends AppCompatActivity {

    private TextView tvStatus, tvAudit, tvLogs;
    private ScrollView scrollLog;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Process logcatProcess;
    private boolean isMonitoring = false;

    /**
     * This method is hooked by the modern libxposed class (MainHook).
     * The module will override this to return 'true' if active.
     */
    public boolean isModuleActive() {
        return false; 
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvAudit = findViewById(R.id.tvAudit); 
        tvLogs = findViewById(R.id.tvLogs);   
        scrollLog = findViewById(R.id.scrollLog);

        Button btnFix = findViewById(R.id.btnFix);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnExport = findViewById(R.id.btnExport);

        if (tvLogs != null) tvLogs.setTextIsSelectable(true);

        updateLSPosedStatus();
        startAuditLoop();

        btnFix.setOnClickListener(v -> {
            new Thread(() -> {
                shell.applyRootFix();
                handler.post(() -> Toast.makeText(this, "Fix Applied", Toast.LENGTH_SHORT).show());
            }).start();
        });

        btnReset.setOnClickListener(v -> {
            Toast.makeText(this, "Clearing Cache & Restarting UI...", Toast.LENGTH_LONG).show();
            new Thread(shell::clearSettingsCache).start();
        });

        btnStart.setOnClickListener(v -> startLogcatMonitor());
        btnStop.setOnClickListener(v -> stopLogcatMonitor());
        btnClear.setOnClickListener(v -> {
            if (tvLogs != null) tvLogs.setText("--- Terminal Cleared ---\n");
        });
        btnExport.setOnClickListener(v -> exportRealLogs());
    }

    private void startAuditLoop() {
        runSystemAudit();
        // Periodically refresh the audit to reflect real-time system changes
        handler.postDelayed(this::startAuditLoop, 3000);
    }

    /**
     * Spawns a background thread to monitor system logs filtered for gesture engine events.
     */
    private void startLogcatMonitor() {
        if (isMonitoring) return;
        isMonitoring = true;
        if (tvLogs != null) tvLogs.append("\n[Log Monitor Started]\n");

        new Thread(() -> {
            try {
                Runtime.getRuntime().exec(new String[]{"su", "-c", "logcat -c"}).waitFor();
                String cmd = "logcat HGS_LOG:V NavigationBar:V EdgeBackGestureHandler:V MiuiEdgeBackGestureHandler:V GestureStubView:V InputDispatcher:E HwcComposer:E NPVCInjector:E *:S";
                logcatProcess = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
                String line;
                while (isMonitoring && (line = reader.readLine()) != null) {
                    final String logLine = line;
                    handler.post(() -> {
                        if (tvLogs != null) {
                            tvLogs.append(logLine + "\n");
                            if (scrollLog != null) scrollLog.fullScroll(ScrollView.FOCUS_DOWN);
                            
                            // Prevent memory overflow in long monitoring sessions
                            if (tvLogs.getLineCount() > 2000) {
                                String current = tvLogs.getText().toString();
                                tvLogs.setText("[Buffer Purged]\n" + current.substring(current.length() / 2));
                            }
                        }
                    });
                }
            } catch (Exception e) {
                handler.post(() -> { if (tvLogs != null) tvLogs.append("Terminal Error: " + e.getMessage() + "\n"); });
            }
        }).start();
    }

    private void stopLogcatMonitor() {
        isMonitoring = false;
        if (logcatProcess != null) {
            logcatProcess.destroy();
            logcatProcess = null;
        }
    }

    /**
     * Checks core system properties and settings to verify fix effectiveness.
     */
    private void runSystemAudit() {
        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("<b>UNIVERSAL HYPEROS AUDIT</b><br/>");
            report.append(formatAuditLine("AOSP Overlay", checkCommandOutput("cmd overlay list | grep gestural").contains("[x]") ? 1 : 0));
            String brand = android.os.Build.BRAND.toLowerCase();
            report.append(formatAuditLine("HyperOS Core", (brand.contains("xiaomi") || brand.contains("poco") || brand.contains("redmi")) ? 1 : 0));
            String navMode = checkCommandOutput("settings get secure navigation_mode").trim();
            report.append(formatAuditLine("Nav Mode (2)", navMode.equals("2") ? 1 : 0));
            report.append(formatAuditLine("LSPosed Hook", isModuleActive() ? 1 : 0));

            handler.post(() -> {
                if (tvAudit != null) {
                    tvAudit.setText(Html.fromHtml(report.toString(), Html.FROM_HTML_MODE_COMPACT));
                }
                updateLSPosedStatus();
            });
        }).start();
    }

    private String formatAuditLine(String label, int state) {
        String color = (state == 1) ? "#4CAF50" : (state == 0 ? "#F44336" : "#FFEB3B");
        String icon = (state == 1) ? "✔ " : (state == 0 ? "✘ " : "❓ ");
        return "<font color=\"" + color + "\">" + icon + label + "</font><br/>";
    }

    private String checkCommandOutput(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            return (line != null) ? line : "null";
        } catch (Exception e) { return "err"; }
    }

    /**
     * Shares the current terminal buffer as a text file using FileProvider.
     */
    private void exportRealLogs() {
        try {
            File logFile = new File(getCacheDir(), "hgs_diagnostic_report.txt");
            FileOutputStream stream = new FileOutputStream(logFile);
            if (tvLogs != null) stream.write(tvLogs.getText().toString().getBytes());
            stream.close();
        
            Uri uri = FileProvider.getUriForFile(this, "com.sah.hgs.fileprovider", logFile);
            Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Report"));
        } catch (Exception e) { Toast.makeText(this, "Export Failed", Toast.LENGTH_SHORT).show(); }
    }

    private void updateLSPosedStatus() {
        if (tvStatus != null) {
            boolean active = isModuleActive();
            tvStatus.setText("LSPosed: " + (active ? "ACTIVE" : "INACTIVE"));
            tvStatus.setTextColor(active ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        }
    }

    @Override
    protected void onDestroy() {
        stopLogcatMonitor();
        super.onDestroy();
    }
}