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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.View;

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
        TextInputLayout inputLayout = findViewById(R.id.repeatInputLayout);
        String[] labels = getResources().getStringArray(R.array.repeat_labels);
        final int[] values = getResources().getIntArray(R.array.repeat_values);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        dropdown.setAdapter(adapter);
        dropdown.setThreshold(0);

        SharedPreferences prefs = getSharedPreferences("rq_prefs", MODE_PRIVATE);
        int selectedValue = prefs.getInt("repeat.count", 1);
        if (selectedValue == -1) {
            dropdown.setText("∞", false);
        } else {
            dropdown.setText(String.valueOf(selectedValue), false);
        }

        dropdown.setOnItemClickListener((parent, view, position, id) -> {
            int value = values[position];
            persistRepeat(prefs, value);
            clearError(inputLayout);
        });

        dropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateAndPersistTyped(dropdown, inputLayout, prefs);
            }
        });

        dropdown.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                validateAndPersistTyped(dropdown, inputLayout, prefs);
                return true;
            }
            return false;
        });

        dropdown.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear error while user edits
                clearError(inputLayout);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void validateAndPersistTyped(AutoCompleteTextView dropdown, TextInputLayout inputLayout, SharedPreferences prefs) {
        String text = dropdown.getText() != null ? dropdown.getText().toString().trim() : "";
        if (text.isEmpty()) {
            showError(inputLayout, "Enter a repeat count or choose ∞");
            return;
        }
        if ("∞".equals(text)) {
            persistRepeat(prefs, -1);
            clearError(inputLayout);
            return;
        }
        try {
            int value = Integer.parseInt(text);
            if (value < 1 || value > 9999) {
                showError(inputLayout, "Enter a value between 1 and 9999");
                return;
            }
            persistRepeat(prefs, value);
            clearError(inputLayout);
        } catch (NumberFormatException e) {
            showError(inputLayout, "Invalid number");
        }
    }

    private void persistRepeat(SharedPreferences prefs, int value) {
        prefs.edit().putInt("repeat.count", value).apply();
    }

    private void showError(TextInputLayout layout, String message) {
        layout.setError(message);
    }

    private void clearError(TextInputLayout layout) {
        layout.setError(null);
        layout.setErrorEnabled(false);
    }
}
