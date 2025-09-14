package com.repeatquran.qa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.repeatquran.R;
import com.repeatquran.playback.PlaybackService;

public class QAActivity extends AppCompatActivity {
    private EditText editCount;
    private EditText editDelay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qa);

        editCount = findViewById(R.id.editSpamCount);
        editDelay = findViewById(R.id.editSpamDelayMs);

        findViewById(R.id.btnSpamPlayPause).setOnClickListener(v -> spamControls(true));
        findViewById(R.id.btnSpamNextPrev).setOnClickListener(v -> spamControls(false));

        findViewById(R.id.btnSimFocusLoss).setOnClickListener(v -> sendService(PlaybackService.ACTION_SIMULATE_FOCUS_LOSS));
        findViewById(R.id.btnSimFocusGain).setOnClickListener(v -> sendService(PlaybackService.ACTION_SIMULATE_FOCUS_GAIN));

        findViewById(R.id.btnSwitchReciters).setOnClickListener(v -> switchReciters());
        findViewById(R.id.btnStartInfiniteSoak).setOnClickListener(v -> startInfiniteSoak());
    }

    private void spamControls(boolean playPause) {
        int n = parseIntSafe(editCount, 10);
        int delay = parseIntSafe(editDelay, 150);
        new Thread(() -> {
            for (int i = 0; i < n; i++) {
                Intent it = new Intent(this, PlaybackService.class);
                it.setAction(playPause ? (i % 2 == 0 ? PlaybackService.ACTION_PLAY : PlaybackService.ACTION_PAUSE)
                        : (i % 2 == 0 ? PlaybackService.ACTION_NEXT : PlaybackService.ACTION_PREV));
                if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(it); else startService(it);
                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private void sendService(String action) {
        Intent it = new Intent(this, PlaybackService.class);
        it.setAction(action);
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(it); else startService(it);
    }

    private void switchReciters() {
        String[] names = getResources().getStringArray(R.array.reciter_names);
        String[] ids = getResources().getStringArray(R.array.reciter_ids);
        if (ids.length < 2) {
            Toast.makeText(this, "Need at least 2 reciters", Toast.LENGTH_SHORT).show();
            return;
        }
        // Toggle between first two reciters
        String current = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", ids[0]);
        String newOrder = current.startsWith(ids[0]) ? ids[1] + "," + ids[0] : ids[0] + "," + ids[1];
        getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putString("reciters.order", newOrder).apply();
        Toast.makeText(this, "Reciters switched to: " + newOrder, Toast.LENGTH_SHORT).show();
    }

    private void startInfiniteSoak() {
        // Start a simple ∞ repeat of 001:001 for 2 minutes
        Intent i = new Intent(this, PlaybackService.class);
        i.setAction(PlaybackService.ACTION_LOAD_SINGLE);
        i.putExtra("sura", 1);
        i.putExtra("ayah", 1);
        i.putExtra("repeat", -1);
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        Toast.makeText(this, "Soak started: ∞ repeat for 2 minutes", Toast.LENGTH_SHORT).show();
        // Stop after 2 minutes
        findViewById(R.id.btnStartInfiniteSoak).postDelayed(() -> sendService(PlaybackService.ACTION_PAUSE), 120_000);
    }

    private int parseIntSafe(EditText e, int def) {
        try { return Integer.parseInt(e.getText()==null?"":e.getText().toString().trim()); } catch (Exception ex) { return def; }
    }
}

