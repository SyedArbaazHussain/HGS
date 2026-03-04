package com.hyperos.gesturefix;

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

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvAudit, tvLogs;
    private ScrollView scrollLog;
    private final Handler handler = new Handler(Looper.getMainLooper());
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
        tvAudit = findViewById(R.id.tvAudit); 
        tvLogs = findViewById(R.id.tvLogs);   
        scrollLog = findViewById(R.id.scrollLog);

        Button btnFix = findViewById(R.id.btnFix);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnExport = findViewById(R.id.btnExport);

        if (tvLogs != null) {
            tvLogs.setTextIsSelectable(true);
        }

        updateLSPosedStatus();
        startAuditLoop(); // Persistent dashboard updates

        if (btnFix != null) {
            btnFix.setOnClickListener(v -> {
                new Thread(() -> {
                    ShellUtils.applyRootFix();
                    handler.post(() -> Toast.makeText(this, "Enforcing Gestures...", Toast.LENGTH_SHORT).show());
                }).start();
            });
        }

        if (btnStart != null) {
            btnStart.setOnClickListener(v -> startLogcatMonitor());
        }

        if (btnStop != null) {
            btnStop.setOnClickListener(v -> stopLogcatMonitor());
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                if (tvLogs != null) tvLogs.setText("--- Terminal Cleared ---\n");
            });
        }

        if (btnExport != null) {
            btnExport.setOnClickListener(v -> exportRealLogs());
        }
    }

    private void startAuditLoop() {
        runSystemAudit();
        // Refresh every 3 seconds to keep dashboard state constant
        handler.postDelayed(this::startAuditLoop, 3000);
    }

    private void startLogcatMonitor() {
        if (isMonitoring) return;
        isMonitoring = true;
        if (tvLogs != null) tvLogs.append("\n[Filtering logs for Gesture Engine...]\n");

        new Thread(() -> {
            try {
                // AGGRESSIVE FILTER: Only allows Gesture, Input, Hardware Composer, and our Module logs.
                // Everything else (*:S) is silenced to stop the noise.
                String cmd = "logcat HGS_LOG:V NavigationBar:V EdgeBackGestureHandler:V MiuiEdgeBackGestureHandler:V GestureStubView:V InputDispatcher:E HwcComposer:E *:S";
                logcatProcess = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
                String line;

                while (isMonitoring && (line = reader.readLine()) != null) {
                    final String logLine = line;
                    handler.post(() -> {
                        if (tvLogs != null) {
                            tvLogs.append(logLine + "\n");
                            if (scrollLog != null) scrollLog.fullScroll(ScrollView.FOCUS_DOWN);
                            
                            if (tvLogs.getLineCount() > 2000) {
                                String current = tvLogs.getText().toString();
                                tvLogs.setText("[Buffer Purged]\n" + current.substring(current.length() / 2));
                            }
                        }
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    if (tvLogs != null) tvLogs.append("Terminal Error: " + e.getMessage() + "\n");
                });
            }
        }).start();
    }

    private void stopLogcatMonitor() {
        isMonitoring = false;
        if (logcatProcess != null) {
            logcatProcess.destroy();
            logcatProcess = null;
        }
        if (tvLogs != null) tvLogs.append("[Monitor Stopped]\n");
    }

    private void runSystemAudit() {
        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("<b>GESTURE DASHBOARD</b><br/>");
            
            report.append(formatAuditLine("AOSP Overlay", checkCommandOutput("cmd overlay list | grep gestural").contains("[x]") ? 1 : 0));
            
            String navMode = checkCommandOutput("settings get secure navigation_mode").trim();
            report.append(formatAuditLine("Nav Mode (2)", navMode.equals("2") ? 1 : (navMode.equals("null") ? -1 : 0)));

            String fsgFlag = checkCommandOutput("settings get global force_fsg_nav_bar").trim();
            report.append(formatAuditLine("FSG Global Flag", fsgFlag.equals("1") ? 1 : (fsgFlag.equals("null") ? -1 : 0)));

            String miuiFsg = checkCommandOutput("settings get secure miui_fsg_gesture_status").trim();
            report.append(formatAuditLine("MIUI FSG Status", miuiFsg.equals("1") ? 1 : (miuiFsg.equals("null") ? -1 : 0)));

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
        String color;
        String icon;
        switch (state) {
            case 1: color = "#4CAF50"; icon = "✔ "; break; // Green
            case 0: color = "#F44336"; icon = "✘ "; break; // Red
            default: color = "#FFEB3B"; icon = "❓ "; break; // Yellow
        }
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

    private void exportRealLogs() {
        if (tvLogs == null || tvLogs.getText().toString().isEmpty()) return;
        try {
            File cachePath = new File(getCacheDir(), "logs");
            if (!cachePath.exists()) cachePath.mkdirs();
            File logFile = new File(cachePath, "hgs_diagnostic_report.txt");
            FileOutputStream stream = new FileOutputStream(logFile);
            stream.write(tvLogs.getText().toString().getBytes());
            stream.close();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", logFile);
            Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Diagnostic Report"));
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