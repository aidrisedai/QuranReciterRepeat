package com.repeatquran.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.repeatquran.R;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_settings);
        }

        android.view.View sw = findViewById(R.id.switchHalfSplit);
        if (sw instanceof android.widget.CheckBox) {
            android.widget.CheckBox s = (android.widget.CheckBox) sw;
            boolean saved = getSharedPreferences("rq_prefs", MODE_PRIVATE).getBoolean("ui.half.split", false);
            s.setChecked(saved);
            s.setOnCheckedChangeListener((buttonView, isChecked) -> {
                getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putBoolean("ui.half.split", isChecked).apply();
                java.util.Map<String,Object> ev = new java.util.HashMap<>();
                ev.put("source", "settings"); ev.put("half", String.valueOf(isChecked));
                com.repeatquran.analytics.AnalyticsLogger.get(this).log("half_split_set", ev);
            });
        }

        android.view.View btnDownloads = findViewById(R.id.btnOpenDownloadsFromSettings);
        if (btnDownloads != null) {
            btnDownloads.setOnClickListener(v -> {
                startActivity(new android.content.Intent(this, com.repeatquran.downloads.DownloadsActivity.class));
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
