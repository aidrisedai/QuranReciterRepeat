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
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AlertDialog;

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

        findViewById(R.id.btnLoadAyah).setOnClickListener(v -> {
            TextInputLayout surahLayout = findViewById(R.id.surahInputLayout);
            TextInputLayout ayahLayout = findViewById(R.id.ayahInputLayout);
            TextInputLayout repeatLayout = findViewById(R.id.repeatInputLayout);
            TextInputEditText surahEdit = findViewById(R.id.editSurah);
            TextInputEditText ayahEdit = findViewById(R.id.editAyah);
            AutoCompleteTextView repeatDropdown = findViewById(R.id.repeatDropdown);

            clearError(surahLayout);
            clearError(ayahLayout);

            int surah = parseIntSafe(surahEdit);
            int ayah = parseIntSafe(ayahEdit);
            if (surah < 1 || surah > 114) { showError(surahLayout, "Enter 1..114"); return; }
            if (ayah < 1) { showError(ayahLayout, "Enter >=1"); return; }

            // Read current repeat text and persist so the service uses the latest value
            String repeatText = repeatDropdown.getText() != null ? repeatDropdown.getText().toString().trim() : "";
            int repeat;
            if (repeatText.isEmpty()) {
                showError(repeatLayout, "Enter repeat or choose ∞");
                return;
            } else if ("∞".equals(repeatText)) {
                repeat = -1;
            } else {
                try {
                    repeat = Integer.parseInt(repeatText);
                } catch (NumberFormatException e) {
                    showError(repeatLayout, "Invalid repeat number");
                    return;
                }
                if (repeat < 1 || repeat > 9999) {
                    showError(repeatLayout, "1..9999 only");
                    return;
                }
            }
            // Persist the latest repeat selection
            getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putInt("repeat.count", repeat).apply();
            String msg = "Loading Surah " + surah + ", Ayah " + ayah + " (repeat=" + (repeat==-1?"∞":repeat) + ")";
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();

            View btn = findViewById(R.id.btnLoadAyah);
            btn.setEnabled(false);
            btn.postDelayed(() -> btn.setEnabled(true), 800);

            Intent intent = new Intent(this, PlaybackService.class);
            intent.setAction(PlaybackService.ACTION_LOAD_SINGLE);
            intent.putExtra("sura", surah);
            intent.putExtra("ayah", ayah);
            intent.putExtra("repeat", repeat);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // No auto-start; user will tap Play or Load Ayah.
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

        // Load Range click
        findViewById(R.id.btnLoadRange).setOnClickListener(v -> {
            TextInputLayout startSurahLayout = findViewById(R.id.startSurahLayout);
            TextInputLayout startAyahLayout = findViewById(R.id.startAyahLayout);
            TextInputLayout endSurahLayout = findViewById(R.id.endSurahLayout);
            TextInputLayout endAyahLayout = findViewById(R.id.endAyahLayout);

            TextInputEditText editStartSurah = findViewById(R.id.editStartSurah);
            TextInputEditText editStartAyah = findViewById(R.id.editStartAyah);
            TextInputEditText editEndSurah = findViewById(R.id.editEndSurah);
            TextInputEditText editEndAyah = findViewById(R.id.editEndAyah);

            clearError(startSurahLayout);
            clearError(startAyahLayout);
            clearError(endSurahLayout);
            clearError(endAyahLayout);

            int ss = parseIntSafe(editStartSurah);
            int sa = parseIntSafe(editStartAyah);
            int es = parseIntSafe(editEndSurah);
            int ea = parseIntSafe(editEndAyah);

            if (!validateSurahAyah(startSurahLayout, startAyahLayout, ss, sa)) return;
            if (!validateSurahAyah(endSurahLayout, endAyahLayout, es, ea)) return;
            if (!isStartBeforeOrEqual(ss, sa, es, ea)) {
                showError(endSurahLayout, "End before start");
                showError(endAyahLayout, "End before start");
                return;
            }

            // Get current repeat
            AutoCompleteTextView rep = findViewById(R.id.repeatDropdown);
            String repText = rep.getText() != null ? rep.getText().toString().trim() : "";
            int repeat;
            if (repText.isEmpty()) {
                showError(inputLayout, "Enter repeat or choose ∞");
                return;
            } else if ("∞".equals(repText)) {
                repeat = -1;
            } else {
                try { repeat = Integer.parseInt(repText); } catch (Exception e) { showError(inputLayout, "Invalid repeat"); return; }
                if (repeat < 1 || repeat > 9999) { showError(inputLayout, "1..9999 only"); return; }
            }
            getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putInt("repeat.count", repeat).apply();

            String msg = "Loading Range " + ss + ":" + sa + " → " + es + ":" + ea + " (repeat=" + (repeat==-1?"∞":repeat) + ")";
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();

            View btn = findViewById(R.id.btnLoadRange);
            btn.setEnabled(false);
            btn.postDelayed(() -> btn.setEnabled(true), 800);

            Intent intent = new Intent(this, PlaybackService.class);
            intent.setAction(PlaybackService.ACTION_LOAD_RANGE);
            intent.putExtra("ss", ss);
            intent.putExtra("sa", sa);
            intent.putExtra("es", es);
            intent.putExtra("ea", ea);
            intent.putExtra("repeat", repeat);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        });

        // Choose Reciters (multi-select with ordered numbering)
        findViewById(R.id.btnChooseReciters).setOnClickListener(v -> showReciterPicker());
        renderSelectedReciters();

        // Page input: preload last page
        TextInputEditText editPage = findViewById(R.id.editPage);
        int lastPage = getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("last.page", 1);
        editPage.setText(String.valueOf(lastPage));

        // Load Page button (wires to service in UHW-28)
        findViewById(R.id.btnLoadPage).setOnClickListener(v -> {
            TextInputLayout pageLayout = findViewById(R.id.pageInputLayout);
            clearError(pageLayout);
            int page = parseIntSafe(editPage);
            if (page < 1 || page > 604) {
                showError(pageLayout, "Enter 1–604");
                return;
            }
            getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putInt("last.page", page).apply();
            // Read repeat now
            AutoCompleteTextView rep = findViewById(R.id.repeatDropdown);
            String repText = rep.getText() != null ? rep.getText().toString().trim() : "";
            int repeat = -1;
            if (!"∞".equals(repText)) {
                try { repeat = Integer.parseInt(repText); } catch (Exception e) { repeat = 1; }
                if (repeat < 1) repeat = 1;
            }
            Intent intent = new Intent(this, com.repeatquran.playback.PlaybackService.class);
            intent.setAction(com.repeatquran.playback.PlaybackService.ACTION_LOAD_PAGE);
            intent.putExtra("page", page);
            intent.putExtra("repeat", repeat);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
            android.widget.Toast.makeText(this, "Loading page " + page + " (repeat=" + (repeat==-1?"∞":repeat) + ")", android.widget.Toast.LENGTH_SHORT).show();
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

    private int parseIntSafe(TextInputEditText edit) {
        try {
            return Integer.parseInt(edit.getText()==null?"":edit.getText().toString().trim());
        } catch (Exception e) {
            return -1;
        }
    }

    // ---- Range validation helpers ----
    private boolean validateSurahAyah(TextInputLayout surahL, TextInputLayout ayahL, int surah, int ayah) {
        if (surah < 1 || surah > 114) { showError(surahL, "Surah 1..114"); return false; }
        int maxAyah = getAyahCount(surah);
        if (ayah < 1 || ayah > maxAyah) { showError(ayahL, "Ayah 1.." + maxAyah); return false; }
        return true;
    }

    private boolean isStartBeforeOrEqual(int ss, int sa, int es, int ea) {
        if (ss < es) return true;
        if (ss > es) return false;
        return sa <= ea;
    }

    private int getAyahCount(int surah) {
        // Surah 1..114
        return AYAH_COUNTS[surah - 1];
    }

    private static final int[] AYAH_COUNTS = new int[] {
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
        5, 4, 5, 6
    };

    // ---- Reciter multi-select ----
    private void showReciterPicker() {
        String[] names = getResources().getStringArray(R.array.reciter_names);
        String[] ids = getResources().getStringArray(R.array.reciter_ids);
        SharedPreferences prefs = getSharedPreferences("rq_prefs", MODE_PRIVATE);
        String saved = prefs.getString("reciters.order", "");
        java.util.List<String> order = new java.util.ArrayList<>();
        if (!saved.isEmpty()) {
            for (String s : saved.split(",")) if (!s.isEmpty()) order.add(s);
        }

        // Build a sorted view by name (case-insensitive), while preserving mapping to ids
        Integer[] idx = new Integer[names.length];
        for (int i = 0; i < names.length; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> names[a].compareToIgnoreCase(names[b]));
        String[] sortedNames = new String[names.length];
        String[] sortedIds = new String[ids.length];
        for (int i = 0; i < idx.length; i++) { sortedNames[i] = names[idx[i]]; sortedIds[i] = ids[idx[i]]; }

        boolean[] checked = new boolean[sortedIds.length];
        java.util.Set<String> selectedSet = new java.util.HashSet<>(order);
        for (int i = 0; i < sortedIds.length; i++) checked[i] = selectedSet.contains(sortedIds[i]);

        java.util.List<String> working = new java.util.ArrayList<>(order);
        new AlertDialog.Builder(this)
                .setTitle("Select Reciters")
                .setMultiChoiceItems(sortedNames, checked, (dialog, which, isChecked) -> {
                    String id = sortedIds[which];
                    if (isChecked) {
                        if (!working.contains(id)) working.add(id);
                    } else {
                        working.remove(id);
                    }
                })
                .setPositiveButton("Save", (d, w) -> {
                    String joined = android.text.TextUtils.join(",", working);
                    prefs.edit().putString("reciters.order", joined).apply();
                    renderSelectedReciters();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renderSelectedReciters() {
        String[] names = getResources().getStringArray(R.array.reciter_names);
        String[] ids = getResources().getStringArray(R.array.reciter_ids);
        java.util.Map<String, String> idToName = new java.util.HashMap<>();
        for (int i = 0; i < ids.length; i++) idToName.put(ids[i], names[i]);
        SharedPreferences prefs = getSharedPreferences("rq_prefs", MODE_PRIVATE);
        String saved = prefs.getString("reciters.order", "");
        StringBuilder sb = new StringBuilder();
        if (saved.isEmpty()) {
            sb.append("Selected reciters: (none)");
        } else {
            String[] parts = saved.split(",");
            sb.append("Selected reciters:\n");
            int n = 1;
            for (String id : parts) {
                String label = idToName.getOrDefault(id, id);
                sb.append(n++).append(". ").append(label).append("\n");
            }
        }
        android.widget.TextView tv = findViewById(R.id.txtReciters);
        tv.setText(sb.toString().trim());
    }
}
