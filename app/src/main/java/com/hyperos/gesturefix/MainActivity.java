package com.hyperos.gesturefix;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // This method is hooked by the Spoofer class above
    public boolean isModuleActive() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvStatus = findViewById(R.id.tvStatus);

        if (isModuleActive()) {
            tvStatus.setText("MODULE ACTIVE\nGestures Unlocked");
            tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Material Green
        } else {
            tvStatus.setText("MODULE INACTIVE\nCheck LSPosed Scope");
            tvStatus.setTextColor(Color.RED);
        }
    }
}