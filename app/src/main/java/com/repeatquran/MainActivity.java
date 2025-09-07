package com.repeatquran;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.repeatquran.playback.PlaybackService;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request notifications permission on Android 13+ so we can show the media notification
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        findViewById(R.id.btnPlay).setOnClickListener(v -> sendServiceAction(PlaybackService.ACTION_PLAY));
        findViewById(R.id.btnPause).setOnClickListener(v -> sendServiceAction(PlaybackService.ACTION_PAUSE));
        findViewById(R.id.btnNext).setOnClickListener(v -> sendServiceAction(PlaybackService.ACTION_NEXT));
        findViewById(R.id.btnPrev).setOnClickListener(v -> sendServiceAction(PlaybackService.ACTION_PREV));

        setupRepeatDropdown();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start foreground playback service for background-safe playback (UHW-5).
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_START);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void sendServiceAction(String action) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void setupRepeatDropdown() {
        AutoCompleteTextView dropdown = findViewById(R.id.repeatDropdown);
        String[] labels = getResources().getStringArray(R.array.repeat_labels);
        final int[] values = getResources().getIntArray(R.array.repeat_values);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        dropdown.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("rq_prefs", MODE_PRIVATE);
        int selectedValue = prefs.getInt("repeat.count", 1);
        // find index by value
        int selIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == selectedValue) { selIndex = i; break; }
            if (selectedValue == -1 && values[i] == -1) { selIndex = i; break; }
        }
        dropdown.setText(labels[selIndex], false);

        dropdown.setOnItemClickListener((parent, view, position, id) -> {
            int value = values[position];
            prefs.edit().putInt("repeat.count", value).apply();
        });
    }
}
