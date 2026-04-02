package com.sah;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class main extends AppCompatActivity {

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

        if (tvLogs != null) tvLogs.setTextIsSelectable(true);

        setupButtons();
        updateLSPosedStatus();
        startAuditLoop();
    }

    private void setupButtons() {
        findViewById(R.id.btnFix).setOnClickListener(v -> new Thread(() -> {
            shell.applyRootFix();
            handler.post(() -> Toast.makeText(this, "Fix Applied", Toast.LENGTH_SHORT).show());
        }).start());

        findViewById(R.id.btnReset).setOnClickListener(v -> {
            Toast.makeText(this, "Restarting UI...", Toast.LENGTH_LONG).show();
            new Thread(shell::clearSettingsCache).start();
        });

        findViewById(R.id.btnStart).setOnClickListener(v -> startLogcatMonitor());
        findViewById(R.id.btnStop).setOnClickListener(v -> stopLogcatMonitor());
        findViewById(R.id.btnClear).setOnClickListener(v -> {
            if (tvLogs != null) tvLogs.setText("--- Terminal Cleared ---\n");
        });
        findViewById(R.id.btnExport).setOnClickListener(v -> exportRealLogs());
    }

    private void startAuditLoop() {
        runSystemAudit();
        handler.postDelayed(this::startAuditLoop, 3000);
    }

    private void startLogcatMonitor() {
        if (isMonitoring) return;
        isMonitoring = true;
        if (tvLogs != null) tvLogs.append("\n[Log Monitor Started]\n");

        new Thread(() -> {
            try {
                Runtime.getRuntime().exec(new String[]{"su", "-c", "logcat -c"}).waitFor();
                String filter = "HGS_Hook:V NavigationBar:V EdgeBackGestureHandler:V *:S";
                logcatProcess = Runtime.getRuntime().exec(new String[]{"su", "-c", "logcat " + filter});
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
                String line;
                while (isMonitoring && (line = reader.readLine()) != null) {
                    final String logLine = line;
                    handler.post(() -> {
                        if (tvLogs != null) {
                            tvLogs.append(logLine + "\n");
                            if (scrollLog != null) scrollLog.fullScroll(ScrollView.FOCUS_DOWN);
                            if (tvLogs.getLineCount() > 1500) tvLogs.setText("[Buffer Purged]\n");
                        }
                    });
                }
            } catch (Exception e) {
                handler.post(() -> { if (tvLogs != null) tvLogs.append("Error: " + e.getMessage() + "\n"); });
            } finally {
                stopLogcatMonitor();
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

    private void runSystemAudit() {
        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("<b>HYPEROS GESTURE AUDIT</b><br/>");
            
            boolean overlayActive = checkCommandOutput("cmd overlay list | grep gestural").contains("[x]");
            report.append(formatAuditLine("AOSP Overlay", overlayActive ? 1 : 0));
            
            String navMode = checkCommandOutput("settings get secure navigation_mode").trim();
            report.append(formatAuditLine("Nav Mode (2)", navMode.equals("2") ? 1 : 0));
            
            report.append(formatAuditLine("LSPosed Hook", isModuleActive() ? 1 : 0));

            handler.post(() -> {
                if (tvAudit != null && !isFinishing()) {
                    tvAudit.setText(Html.fromHtml(report.toString(), Html.FROM_HTML_MODE_COMPACT));
                }
                updateLSPosedStatus();
            });
        }).start();
    }

    private String formatAuditLine(String label, int state) {
        String color = (state == 1) ? "#4CAF50" : "#F44336";
        String icon = (state == 1) ? "✔ " : "✘ ";
        return "<font color=\"" + color + "\">" + icon + label + "</font><br/>";
    }

    private String checkCommandOutput(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            p.destroy();
            return (line != null) ? line : "null";
        } catch (Exception e) { return "err"; }
    }

    private void exportRealLogs() {
        try {
            File logFile = new File(getCacheDir(), "hgs_report.txt");
            FileOutputStream stream = new FileOutputStream(logFile);
            if (tvLogs != null) stream.write(tvLogs.getText().toString().getBytes());
            stream.close();
            Uri uri = FileProvider.getUriForFile(this, "com.sah.hgs.fileprovider", logFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}