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
import android.widget.Filterable;
import com.repeatquran.data.SessionRepository;
import com.repeatquran.data.db.SessionEntity;
import com.repeatquran.data.PresetRepository;
import com.repeatquran.data.db.PresetEntity;
import com.repeatquran.ui.ModesPagerAdapter;

public class MainActivity extends AppCompatActivity {
    private android.content.BroadcastReceiver playbackStateReceiver;
    private static final String[] SURAH_NAMES_EN = new String[] {
            "Al-Fatihah", "Al-Baqarah", "Aal Imran", "An-Nisa", "Al-Maidah", "Al-An'am", "Al-A'raf", "Al-Anfal", "At-Tawbah", "Yunus",
            "Hud", "Yusuf", "Ar-Ra'd", "Ibrahim", "Al-Hijr", "An-Nahl", "Al-Isra", "Al-Kahf", "Maryam", "Ta-Ha",
            "Al-Anbiya", "Al-Hajj", "Al-Mu'minun", "An-Nur", "Al-Furqan", "Ash-Shu'ara", "An-Naml", "Al-Qasas", "Al-Ankabut", "Ar-Rum",
            "Luqman", "As-Sajdah", "Al-Ahzab", "Saba", "Fatir", "Ya-Sin", "As-Saffat", "Sad", "Az-Zumar", "Ghafir",
            "Fussilat", "Ash-Shura", "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah", "Al-Ahqaf", "Muhammad", "Al-Fath", "Al-Hujurat", "Qaf",
            "Adh-Dhariyat", "At-Tur", "An-Najm", "Al-Qamar", "Ar-Rahman", "Al-Waqi'ah", "Al-Hadid", "Al-Mujadila", "Al-Hashr", "Al-Mumtahanah",
            "As-Saff", "Al-Jumu'ah", "Al-Munafiqun", "At-Taghabun", "At-Talaq", "At-Tahrim", "Al-Mulk", "Al-Qalam", "Al-Haqqah", "Al-Ma'arij",
            "Nuh", "Al-Jinn", "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah", "Al-Insan", "Al-Mursalat", "An-Naba", "An-Nazi'at", "Abasa",
            "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Inshiqaq", "Al-Buruj", "At-Tariq", "Al-A'la", "Al-Ghashiyah", "Al-Fajr", "Al-Balad",
            "Ash-Shams", "Al-Layl", "Ad-Duha", "Ash-Sharh", "At-Tin", "Al-Alaq", "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah", "Al-Adiyat",
            "Al-Qari'ah", "At-Takathur", "Al-Asr", "Al-Humazah", "Al-Fil", "Quraysh", "Al-Ma'un", "Al-Kawthar", "Al-Kafirun", "An-Nasr",
            "Al-Masad", "Al-Ikhlas", "Al-Falaq", "An-Nas"
    };

    private String surahName(int surah) {
        if (surah >= 1 && surah <= SURAH_NAMES_EN.length) return SURAH_NAMES_EN[surah - 1];
        return "";
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // One-time onboarding
        boolean seen = getSharedPreferences("rq_prefs", MODE_PRIVATE).getBoolean("onboarding.seen", false);
        if (!seen) {
            android.content.Intent i = new android.content.Intent(this, com.repeatquran.onboarding.OnboardingActivity.class);
            startActivity(i);
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        // Toolbar menu handling (Remember my mode)
        com.google.android.material.appbar.MaterialToolbar bar = findViewById(R.id.topAppBar);
        if (bar != null) {
            SharedPreferences prefsToolbar = getSharedPreferences("rq_prefs", MODE_PRIVATE);
            boolean rememberInit = prefsToolbar.getBoolean("ui.remember.mode", true);
            android.view.MenuItem menuItem = bar.getMenu().findItem(R.id.action_remember_mode);
            if (menuItem != null) menuItem.setChecked(rememberInit);
            bar.setOnMenuItemClickListener(mi -> {
                if (mi.getItemId() == R.id.action_remember_mode) {
                    boolean newVal = !mi.isChecked();
                    mi.setChecked(newVal);
                    getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putBoolean("ui.remember.mode", newVal).apply();
                    return true;
                } else if (mi.getItemId() == R.id.action_settings) {
                    startActivity(new android.content.Intent(this, com.repeatquran.settings.SettingsActivity.class));
                    return true;
                }
                return false;
            });
        }

        // Setup tabs skeleton (Verse | Range | Page | Surah)
        androidx.viewpager2.widget.ViewPager2 pager = findViewById(R.id.modePager);
        if (pager != null) {
            pager.setAdapter(new com.repeatquran.ui.ModesPagerAdapter(this));
            com.google.android.material.tabs.TabLayout tabs = findViewById(R.id.modeTabs);
            if (tabs != null) {
                new com.google.android.material.tabs.TabLayoutMediator(tabs, pager,
                        (tab, position) -> {
                            switch (position) {
                                case 0: tab.setText("Verse"); break;
                                case 1: tab.setText("Range"); break;
                                case 2: tab.setText("Page"); break;
                                case 3: tab.setText("Surah"); break;
                            }
                        }).attach();
            }

            // Restore last mode selection if enabled
            SharedPreferences prefs = getSharedPreferences("rq_prefs", MODE_PRIVATE);
            boolean remember = prefs.getBoolean("ui.remember.mode", true);
            int last = prefs.getInt("ui.last.mode", 0);
            if (remember && last >= 0 && last < 4) pager.setCurrentItem(last, false);

            // Save selection when page changes if enabled
            pager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                @Override public void onPageSelected(int position) {
                    boolean rem = getSharedPreferences("rq_prefs", MODE_PRIVATE).getBoolean("ui.remember.mode", true);
                    if (rem) getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putInt("ui.last.mode", position).apply();
                    java.util.Map<String,Object> ev = new java.util.HashMap<>();
                    ev.put("tab", positionName(position));
                    com.repeatquran.analytics.AnalyticsLogger.get(MainActivity.this).log("tab_selected", ev);
                }
            });
        }

        // Global pills: summary + interactions
        refreshGlobalPills();
        android.view.View chipRepeat = findViewById(R.id.chipRepeat);
        if (chipRepeat != null) {
            chipRepeat.setOnClickListener(v -> {
                android.widget.AutoCompleteTextView dd = findViewById(R.id.repeatDropdown);
                if (dd != null) dd.showDropDown();
            });
        }
        android.view.View chipReciters = findViewById(R.id.chipReciters);
        if (chipReciters != null) {
            chipReciters.setOnClickListener(v -> showReciterPicker());
        }

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
        findViewById(R.id.btnResume).setOnClickListener(v -> sendServiceAction(PlaybackService.ACTION_RESUME));

        // Default: enable Resume; receiver will disable when active playback is present
        android.view.View btnResume = findViewById(R.id.btnResume);
        btnResume.setEnabled(true);

        setupRepeatDropdown();
        // Analytics: app open
        com.repeatquran.analytics.AnalyticsLogger.get(this).log("app_open", java.util.Collections.emptyMap());

        findViewById(R.id.btnLoadAyah).setOnClickListener(v -> {
            TextInputLayout surahLayout = findViewById(R.id.surahInputLayout);
            TextInputLayout ayahLayout = findViewById(R.id.ayahInputLayout);
            TextInputLayout repeatLayout = findViewById(R.id.repeatInputLayout);
            AutoCompleteTextView surahDd = findViewById(R.id.surahSingleDropdown);
            TextInputEditText ayahEdit = findViewById(R.id.editAyah);
            AutoCompleteTextView repeatDropdown = findViewById(R.id.repeatDropdown);

            clearError(surahLayout);
            clearError(ayahLayout);

            String surahText = surahDd.getText() != null ? surahDd.getText().toString().trim() : "";
            if (surahText.length() < 3) { showError(surahLayout, "Select surah"); return; }
            String surahNumStr = surahText.substring(0, 3);
            int surah;
            try { surah = Integer.parseInt(surahNumStr); } catch (Exception e) { showError(surahLayout, "Select surah"); return; }
            if (surah < 1 || surah > 114) { showError(surahLayout, "Select 001..114"); return; }

            int ayah = parseIntSafe(ayahEdit);
            if (ayah < 1) { showError(ayahLayout, "Enter >=1"); return; }
            int maxAyah = getAyahCount(surah);
            if (ayah > maxAyah) { showError(ayahLayout, "Max: " + maxAyah); return; }

            String repeatText = repeatDropdown.getText() != null ? repeatDropdown.getText().toString().trim() : "";
            int repeat;
            if (repeatText.isEmpty()) { showError(repeatLayout, "Enter repeat or choose ∞"); return; }
            if ("∞".equals(repeatText)) { repeat = -1; }
            else {
                try { repeat = Integer.parseInt(repeatText); } catch (NumberFormatException e) { showError(repeatLayout, "Invalid repeat number"); return; }
                if (repeat < 1 || repeat > 9999) { showError(repeatLayout, "1..9999 only"); return; }
            }

            getSharedPreferences("rq_prefs", MODE_PRIVATE).edit()
                    .putInt("repeat.count", repeat)
                    .putInt("last.surah.single", surah)
                    .apply();
            String msg = "Loading Surah " + surahNumStr + " — " + surahName(surah) + ", Ayah " + ayah + " (repeat=" + (repeat==-1?"∞":repeat) + ")";
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

            AutoCompleteTextView ddStartSurah = findViewById(R.id.startSurahDropdown);
            TextInputEditText editStartAyah = findViewById(R.id.editStartAyah);
            AutoCompleteTextView ddEndSurah = findViewById(R.id.endSurahDropdown);
            TextInputEditText editEndAyah = findViewById(R.id.editEndAyah);

            clearError(startSurahLayout);
            clearError(startAyahLayout);
            clearError(endSurahLayout);
            clearError(endAyahLayout);

            // Extract surah numbers from dropdowns (format "NNN — Name" or "NNN")
            String startTxt = ddStartSurah.getText() != null ? ddStartSurah.getText().toString().trim() : "";
            String endTxt = ddEndSurah.getText() != null ? ddEndSurah.getText().toString().trim() : "";
            if (startTxt.length() < 3) { showError(startSurahLayout, "Select start"); return; }
            if (endTxt.length() < 3) { showError(endSurahLayout, "Select end"); return; }
            int ss;
            int es;
            try { ss = Integer.parseInt(startTxt.substring(0,3)); } catch (Exception e) { showError(startSurahLayout, "Select start"); return; }
            try { es = Integer.parseInt(endTxt.substring(0,3)); } catch (Exception e) { showError(endSurahLayout, "Select end"); return; }
            if (ss < 1 || ss > 114) { showError(startSurahLayout, "1..114"); return; }
            if (es < 1 || es > 114) { showError(endSurahLayout, "1..114"); return; }
            int sa = parseIntSafe(editStartAyah);
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
            getSharedPreferences("rq_prefs", MODE_PRIVATE).edit()
                    .putInt("repeat.count", repeat)
                    .putInt("last.surah.range.start", ss)
                    .putInt("last.surah.range.end", es)
                    .apply();
            java.util.Map<String, Object> ev = new java.util.HashMap<>();
            ev.put("repeat", repeat);
            ev.put("type", "range");
            ev.put("ss", ss); ev.put("sa", sa); ev.put("es", es); ev.put("ea", ea);
            com.repeatquran.analytics.AnalyticsLogger.get(this).log("load_range", ev);

            String msg = "Loading Range " + String.format("%03d", ss) + " — " + surahName(ss) + ":" + sa +
                    " → " + String.format("%03d", es) + " — " + surahName(es) + ":" + ea +
                    " (repeat=" + (repeat==-1?"∞":repeat) + ")";
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

        // Preload range surah dropdowns
        setupRangeSurahDropdowns();

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
            java.util.Map<String, Object> ev = new java.util.HashMap<>();
            ev.put("repeat", repeat);
            ev.put("page", page);
            com.repeatquran.analytics.AnalyticsLogger.get(this).log("load_page", ev);
        });

        // Surah dropdown setup
        setupSurahDropdown();
        setupSingleSurahDropdown();
        findViewById(R.id.btnLoadSurah).setOnClickListener(v -> onLoadSurah());

        // Downloads and QA are now accessible from Settings only

        android.view.View savePreset = findViewById(R.id.btnSavePreset);
        if (savePreset != null) savePreset.setVisibility(View.GONE);
        android.view.View presetsTitle = findViewById(R.id.presetsTitle);
        if (presetsTitle != null) presetsTitle.setVisibility(View.GONE);
        android.view.View presetContainer = findViewById(R.id.presetContainer);
        if (presetContainer != null) presetContainer.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playbackStateReceiver == null) {
            playbackStateReceiver = new android.content.BroadcastReceiver() {
                @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
                    boolean active = intent.getBooleanExtra("active", false);
                    android.view.View btn = findViewById(R.id.btnResume);
                    if (btn != null) btn.setEnabled(!active);
                }
            };
        }
        android.content.IntentFilter f = new android.content.IntentFilter(PlaybackService.ACTION_PLAYBACK_STATE);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(playbackStateReceiver, f, android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(playbackStateReceiver, f);
        }
        // Also refresh recent history list
        renderRecentHistory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playbackStateReceiver != null) {
            try { unregisterReceiver(playbackStateReceiver); } catch (Exception ignored) {}
        }
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
        java.util.Map<String, Object> ev = new java.util.HashMap<>();
        ev.put("repeat", value);
        com.repeatquran.analytics.AnalyticsLogger.get(this).log("repeat_set", ev);
        refreshGlobalPills();
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

    // ---- Surah selection ----
    private void setupSurahDropdown() {
        AutoCompleteTextView dd = findViewById(R.id.surahDropdown);
        String[] nums = getResources().getStringArray(R.array.surah_numbers);
        String[] display = new String[nums.length];
        for (int i = 0; i < nums.length; i++) {
            String name = (i < SURAH_NAMES_EN.length) ? SURAH_NAMES_EN[i] : "";
            display[i] = nums[i] + " — " + name;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, display);
        dd.setAdapter(adapter);
        dd.setThreshold(0);
        // preload last surah
        int last = getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("last.surah", 1);
        if (last >= 1 && last <= 114) dd.setText(display[last - 1], false);
    }

    private void setupSingleSurahDropdown() {
        AutoCompleteTextView dd = findViewById(R.id.surahSingleDropdown);
        String[] nums = getResources().getStringArray(R.array.surah_numbers);
        String[] display = new String[nums.length];
        for (int i = 0; i < nums.length; i++) {
            String name = (i < SURAH_NAMES_EN.length) ? SURAH_NAMES_EN[i] : "";
            display[i] = nums[i] + " — " + name;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, display);
        dd.setAdapter(adapter);
        dd.setThreshold(0);
        int last = getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("last.surah.single", 1);
        if (last >= 1 && last <= 114) dd.setText(display[last - 1], false);

        TextInputLayout ayahLayout = findViewById(R.id.ayahInputLayout);
        // Initialize helper for last
        ayahLayout.setHelperText("Max ayah: " + getAyahCount(Math.max(1, Math.min(114, last))));
        dd.setOnItemClickListener((parent, view, position, id) -> {
            int surah = position + 1;
            ayahLayout.setHelperText("Max ayah: " + getAyahCount(surah));
        });
        dd.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() >= 3) {
                    try {
                        int surah = Integer.parseInt(s.subSequence(0, 3).toString());
                        if (surah >= 1 && surah <= 114) ayahLayout.setHelperText("Max ayah: " + getAyahCount(surah));
                    } catch (Exception ignored) {}
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRangeSurahDropdowns() {
        AutoCompleteTextView ddStart = findViewById(R.id.startSurahDropdown);
        AutoCompleteTextView ddEnd = findViewById(R.id.endSurahDropdown);
        String[] nums = getResources().getStringArray(R.array.surah_numbers);
        String[] display = new String[nums.length];
        for (int i = 0; i < nums.length; i++) {
            String name = (i < SURAH_NAMES_EN.length) ? SURAH_NAMES_EN[i] : "";
            display[i] = nums[i] + " — " + name;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, display);
        ddStart.setAdapter(adapter);
        ddStart.setThreshold(0);
        ddEnd.setAdapter(adapter);
        ddEnd.setThreshold(0);
        SharedPreferences prefs = getSharedPreferences("rq_prefs", MODE_PRIVATE);
        int lastStart = prefs.getInt("last.surah.range.start", 1);
        int lastEnd = prefs.getInt("last.surah.range.end", 1);
        if (lastStart >= 1 && lastStart <= 114) ddStart.setText(display[lastStart - 1], false);
        if (lastEnd >= 1 && lastEnd <= 114) ddEnd.setText(display[lastEnd - 1], false);

        TextInputLayout startAyahLayout = findViewById(R.id.startAyahLayout);
        TextInputLayout endAyahLayout = findViewById(R.id.endAyahLayout);
        startAyahLayout.setHelperText("Max ayah: " + getAyahCount(Math.max(1, Math.min(114, lastStart))));
        endAyahLayout.setHelperText("Max ayah: " + getAyahCount(Math.max(1, Math.min(114, lastEnd))));
        ddStart.setOnItemClickListener((p, v, pos, id) -> startAyahLayout.setHelperText("Max ayah: " + getAyahCount(pos + 1)));
        ddEnd.setOnItemClickListener((p, v, pos, id) -> endAyahLayout.setHelperText("Max ayah: " + getAyahCount(pos + 1)));
    }

    // ---- Quick History UI (last 4 sessions) ----

    private void renderRecentHistory() {
        View container = findViewById(R.id.historyContainer);
        if (!(container instanceof android.widget.LinearLayout)) return;
        android.widget.LinearLayout ll = (android.widget.LinearLayout) container;
        ll.removeAllViews();
        // Load sessions off-main and then render
        new Thread(() -> {
            SessionRepository repo = new SessionRepository(this);
            java.util.List<SessionEntity> latest = repo.getLastSessions(10); // fetch more to filter duplicates
            java.util.LinkedHashMap<String, SessionEntity> distinct = new java.util.LinkedHashMap<>();
            for (SessionEntity e : latest) {
                String key;
                if ("single".equals(e.sourceType)) {
                    key = "single:" + e.startSurah + ":" + e.startAyah;
                } else if ("range".equals(e.sourceType)) {
                    key = "range:" + e.startSurah + ":" + e.startAyah + ":" + e.endSurah + ":" + e.endAyah;
                } else if ("surah".equals(e.sourceType)) {
                    key = "surah:" + (e.startSurah == null ? -1 : e.startSurah);
                } else if ("page".equals(e.sourceType)) {
                    key = "page"; // page number not stored in v1
                } else {
                    key = e.sourceType != null ? e.sourceType : "unknown";
                }
                if (!distinct.containsKey(key)) distinct.put(key, e);
                if (distinct.size() == 2) break;
            }
            java.util.List<SessionEntity> sessions = new java.util.ArrayList<>(distinct.values());
            runOnUiThread(() -> {
                if (sessions.isEmpty()) {
                    android.widget.TextView tv = new android.widget.TextView(this);
                    tv.setText("No recent sessions");
                    ll.addView(tv);
                } else {
                    for (SessionEntity e : sessions) ll.addView(buildHistoryItemView(e));
                }
            });
        }).start();
    }

    private void renderPresets() {
        View container = findViewById(R.id.presetContainer);
        if (!(container instanceof android.widget.LinearLayout)) return;
        android.widget.LinearLayout ll = (android.widget.LinearLayout) container;
        ll.removeAllViews();
        new Thread(() -> {
            PresetRepository repo = new PresetRepository(this);
            java.util.List<PresetEntity> presets = repo.getAll();
            runOnUiThread(() -> {
                if (presets == null || presets.isEmpty()) {
                    android.widget.TextView tv = new android.widget.TextView(this);
                    tv.setText("No presets yet");
                    ll.addView(tv);
                } else {
                    for (PresetEntity p : presets) {
                        ll.addView(buildPresetItemView(p));
                    }
                }
            });
        }).start();
    }

    private View buildPresetItemView(PresetEntity p) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setPadding(8, 8, 8, 8);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.TextView name = new android.widget.TextView(this);
        name.setText(p.name + "  (" + p.sourceType + ")");
        name.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(name);
        android.widget.Button btnPlay = new android.widget.Button(this);
        btnPlay.setText("Play");
        btnPlay.setOnClickListener(v -> playPreset(p));
        row.addView(btnPlay);
        android.widget.Button btnEdit = new android.widget.Button(this);
        btnEdit.setText("Edit");
        btnEdit.setOnClickListener(v -> editPreset(p));
        row.addView(btnEdit);
        android.widget.Button btnDel = new android.widget.Button(this);
        btnDel.setText("Del");
        btnDel.setOnClickListener(v -> deletePreset(p));
        row.addView(btnDel);
        return row;
    }

    private void playPreset(PresetEntity p) {
        Intent intent = new Intent(this, PlaybackService.class);
        if ("single".equals(p.sourceType) && p.startSurah != null && p.startAyah != null) {
            intent.setAction(PlaybackService.ACTION_LOAD_SINGLE);
            intent.putExtra("sura", p.startSurah);
            intent.putExtra("ayah", p.startAyah);
        } else if ("range".equals(p.sourceType) && p.startSurah != null && p.startAyah != null && p.endSurah != null && p.endAyah != null) {
            intent.setAction(PlaybackService.ACTION_LOAD_RANGE);
            intent.putExtra("ss", p.startSurah);
            intent.putExtra("sa", p.startAyah);
            intent.putExtra("es", p.endSurah);
            intent.putExtra("ea", p.endAyah);
        } else if ("page".equals(p.sourceType) && p.page != null) {
            intent.setAction(PlaybackService.ACTION_LOAD_PAGE);
            intent.putExtra("page", p.page);
        } else if ("surah".equals(p.sourceType) && p.startSurah != null) {
            intent.setAction(PlaybackService.ACTION_LOAD_SURAH);
            intent.putExtra("surah", p.startSurah);
        } else {
            return;
        }
        intent.putExtra("repeat", p.repeatCount);
        // Use saved reciters
        if (p.recitersCsv != null) getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putString("reciters.order", p.recitersCsv).apply();
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
    }

    private void editPreset(PresetEntity p) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(p.name);
        input.setHint("Name");
        final android.widget.EditText repeat = new android.widget.EditText(this);
        repeat.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        repeat.setHint("Repeat (-1 = ∞)");
        repeat.setText(String.valueOf(p.repeatCount));
        android.widget.LinearLayout ll = new android.widget.LinearLayout(this);
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);
        ll.setPadding(24, 16, 24, 0);
        ll.addView(input);
        ll.addView(repeat);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Edit Preset")
                .setView(ll)
                .setPositiveButton("Save", (d, w) -> {
                    p.name = input.getText() == null ? p.name : input.getText().toString().trim();
                    try { p.repeatCount = Integer.parseInt(repeat.getText()==null?String.valueOf(p.repeatCount):repeat.getText().toString().trim()); } catch (Exception ignored) {}
                    p.updatedAt = System.currentTimeMillis();
                    new Thread(() -> { new PresetRepository(this).update(p); runOnUiThread(this::renderPresets); }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePreset(PresetEntity p) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete preset?")
                .setMessage(p.name)
                .setPositiveButton("Delete", (d, w) -> new Thread(() -> { new PresetRepository(this).delete(p); runOnUiThread(this::renderPresets); }).start())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onSavePreset() {
        String[] types = new String[]{"single", "range", "page", "surah"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Save Preset")
                .setItems(types, (dialog, which) -> doSavePreset(types[which]))
                .show();
    }

    private void doSavePreset(String type) {
        PresetEntity p = new PresetEntity();
        p.name = "Preset " + android.text.format.DateFormat.format("MM-dd HH:mm", System.currentTimeMillis());
        p.sourceType = type;
        p.recitersCsv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        p.repeatCount = getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1);
        p.createdAt = p.updatedAt = System.currentTimeMillis();
        boolean ok = false;
        if ("single".equals(type)) {
            AutoCompleteTextView surahDd = findViewById(R.id.surahSingleDropdown);
            com.google.android.material.textfield.TextInputEditText ayahEdit = findViewById(R.id.editAyah);
            String text = surahDd.getText() != null ? surahDd.getText().toString().trim() : "";
            if (text.length() >= 3) try { p.startSurah = Integer.parseInt(text.substring(0,3)); } catch (Exception ignored) {}
            try { p.startAyah = Integer.parseInt(ayahEdit.getText()==null?"":ayahEdit.getText().toString().trim()); } catch (Exception ignored) {}
            ok = p.startSurah != null && p.startAyah != null && p.startSurah >=1 && p.startSurah<=114 && p.startAyah>=1;
        } else if ("range".equals(type)) {
            AutoCompleteTextView s1 = findViewById(R.id.startSurahDropdown);
            AutoCompleteTextView s2 = findViewById(R.id.endSurahDropdown);
            com.google.android.material.textfield.TextInputEditText a1 = findViewById(R.id.editStartAyah);
            com.google.android.material.textfield.TextInputEditText a2 = findViewById(R.id.editEndAyah);
            String t1 = s1.getText() != null ? s1.getText().toString().trim() : "";
            String t2 = s2.getText() != null ? s2.getText().toString().trim() : "";
            if (t1.length()>=3) try { p.startSurah = Integer.parseInt(t1.substring(0,3)); } catch (Exception ignored) {}
            if (t2.length()>=3) try { p.endSurah = Integer.parseInt(t2.substring(0,3)); } catch (Exception ignored) {}
            try { p.startAyah = Integer.parseInt(a1.getText()==null?"":a1.getText().toString().trim()); } catch (Exception ignored) {}
            try { p.endAyah = Integer.parseInt(a2.getText()==null?"":a2.getText().toString().trim()); } catch (Exception ignored) {}
            ok = p.startSurah != null && p.endSurah != null && p.startAyah != null && p.endAyah != null;
        } else if ("page".equals(type)) {
            com.google.android.material.textfield.TextInputEditText editPage = findViewById(R.id.editPage);
            try { p.page = Integer.parseInt(editPage.getText()==null?"":editPage.getText().toString().trim()); } catch (Exception ignored) {}
            ok = p.page != null && p.page >= 1 && p.page <= 604;
        } else if ("surah".equals(type)) {
            AutoCompleteTextView dd = findViewById(R.id.surahDropdown);
            String txt = dd.getText() != null ? dd.getText().toString().trim() : "";
            if (txt.length()>=3) try { p.startSurah = Integer.parseInt(txt.substring(0,3)); } catch (Exception ignored) {}
            ok = p.startSurah != null && p.startSurah >= 1 && p.startSurah <= 114;
        }
        if (!ok) {
            android.widget.Toast.makeText(this, "Incomplete inputs for " + type, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        final android.widget.EditText name = new android.widget.EditText(this);
        name.setHint("Name (optional)");
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Name Preset")
                .setView(name)
                .setPositiveButton("Save", (d,w) -> {
                    String n = name.getText()==null?"":name.getText().toString().trim();
                    if (!n.isEmpty()) p.name = n;
                    new Thread(() -> { new PresetRepository(this).insert(p); runOnUiThread(this::renderPresets); }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private View buildHistoryItemView(SessionEntity e) {
        android.widget.LinearLayout item = new android.widget.LinearLayout(this);
        item.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        item.setPadding(pad, pad, pad, pad);
        item.setBackgroundColor(0x0D000000); // light divider background

        String title;
        if ("single".equals(e.sourceType)) {
            String sss = String.format("%03d", e.startSurah);
            title = "Single • " + sss + " — " + surahName(e.startSurah) + ":" + e.startAyah;
        } else if ("range".equals(e.sourceType)) {
            String s1 = String.format("%03d", e.startSurah);
            String s2 = String.format("%03d", e.endSurah);
            title = "Range • " + s1 + " — " + surahName(e.startSurah) + ":" + e.startAyah +
                    " → " + s2 + " — " + surahName(e.endSurah) + ":" + e.endAyah;
        } else if ("page".equals(e.sourceType)) {
            title = "Page • (number not stored)";
        } else if ("surah".equals(e.sourceType)) {
            String sss = String.format("%03d", e.startSurah != null ? e.startSurah : 0);
            title = "Surah • " + sss + " — " + surahName(e.startSurah != null ? e.startSurah : 1);
        } else {
            title = "Provider";
        }

        android.widget.TextView tvTitle = new android.widget.TextView(this);
        tvTitle.setText(title);
        tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
        item.addView(tvTitle);

        // Reciters summary
        String recSummary = recitersSummary(e.recitersCsv);
        android.widget.TextView tvSub = new android.widget.TextView(this);
        tvSub.setText("Reciters: " + recSummary + "  •  Repeat: " + (e.repeatCount == -1 ? "∞" : e.repeatCount));
        item.addView(tvSub);

        // Click to replay where possible
        if (!"provider".equals(e.sourceType)) {
            item.setOnClickListener(v -> replaySession(e));
        }
        return item;
    }

    private String recitersSummary(String csv) {
        if (csv == null || csv.isEmpty()) return "(current selection)";
        String[] ids = csv.split(",");
        String[] namesArr = getResources().getStringArray(R.array.reciter_names);
        String[] idsArr = getResources().getStringArray(R.array.reciter_ids);
        java.util.Map<String,String> map = new java.util.HashMap<>();
        for (int i = 0; i < idsArr.length; i++) map.put(idsArr[i], namesArr[i]);
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String id : ids) {
            if (id.isEmpty()) continue;
            String nm = map.get(id);
            names.add(nm != null ? nm : id);
        }
        if (names.isEmpty()) return "(current selection)";
        if (names.size() == 1) return names.get(0);
        if (names.size() == 2) return names.get(0) + ", " + names.get(1);
        return names.get(0) + ", " + names.get(1) + " +" + (names.size() - 2);
    }

    private void replaySession(SessionEntity e) {
        Intent intent = new Intent(this, PlaybackService.class);
        if ("single".equals(e.sourceType) && e.startSurah != null && e.startAyah != null) {
            intent.setAction(PlaybackService.ACTION_LOAD_SINGLE);
            intent.putExtra("sura", e.startSurah);
            intent.putExtra("ayah", e.startAyah);
            intent.putExtra("repeat", e.repeatCount);
        } else if ("range".equals(e.sourceType) && e.startSurah != null && e.startAyah != null && e.endSurah != null && e.endAyah != null) {
            intent.setAction(PlaybackService.ACTION_LOAD_RANGE);
            intent.putExtra("ss", e.startSurah);
            intent.putExtra("sa", e.startAyah);
            intent.putExtra("es", e.endSurah);
            intent.putExtra("ea", e.endAyah);
            intent.putExtra("repeat", e.repeatCount);
        } else if ("surah".equals(e.sourceType) && e.startSurah != null) {
            intent.setAction(PlaybackService.ACTION_LOAD_SURAH);
            intent.putExtra("surah", e.startSurah);
            intent.putExtra("repeat", e.repeatCount);
        } else {
            return; // unsupported replay
        }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
    }

    private void onLoadSurah() {
        TextInputLayout layout = findViewById(R.id.surahSelectLayout);
        AutoCompleteTextView dd = findViewById(R.id.surahDropdown);
        clearError(layout);
        String txt = dd.getText() != null ? dd.getText().toString() : "";
        if (txt.isEmpty() || txt.length() < 3) { showError(layout, "Select a surah"); return; }
        // Extract number prefix
        String numStr = txt.substring(0, 3);
        int surah;
        try { surah = Integer.parseInt(numStr); } catch (Exception e) { showError(layout, "Select a surah"); return; }
        if (surah < 1 || surah > 114) { showError(layout, "Invalid surah"); return; }
        getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putInt("last.surah", surah).apply();
        // Read repeat now
        AutoCompleteTextView rep = findViewById(R.id.repeatDropdown);
        String repText = rep.getText() != null ? rep.getText().toString().trim() : "";
        int repeat = -1;
        if (!"∞".equals(repText)) {
            try { repeat = Integer.parseInt(repText); } catch (Exception e) { repeat = 1; }
            if (repeat < 1) repeat = 1;
        }
        Intent intent = new Intent(this, com.repeatquran.playback.PlaybackService.class);
        intent.setAction(com.repeatquran.playback.PlaybackService.ACTION_LOAD_SURAH);
        intent.putExtra("surah", surah);
        intent.putExtra("repeat", repeat);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        android.widget.Toast.makeText(this, "Loading surah " + numStr + " (repeat=" + (repeat==-1?"∞":repeat) + ")", android.widget.Toast.LENGTH_SHORT).show();
        java.util.Map<String, Object> ev = new java.util.HashMap<>();
        ev.put("repeat", repeat);
        ev.put("surah", surah);
        com.repeatquran.analytics.AnalyticsLogger.get(this).log("load_surah", ev);
    }

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
        refreshGlobalPills();
    }

    private void refreshGlobalPills() {
        android.widget.TextView chipR;
        android.view.View v = findViewById(R.id.chipRepeat);
        if (v instanceof com.google.android.material.chip.Chip) {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) v;
            int repeat = getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1);
            chip.setText("Repeat: " + (repeat == -1 ? "∞" : String.valueOf(repeat)));
        }
        v = findViewById(R.id.chipReciters);
        if (v instanceof com.google.android.material.chip.Chip) {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) v;
            chip.setText("Reciters: " + summarizeReciters());
        }
    }

    private String summarizeReciters() {
        String[] namesArr = getResources().getStringArray(R.array.reciter_names);
        String[] idsArr = getResources().getStringArray(R.array.reciter_ids);
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (int i = 0; i < idsArr.length; i++) map.put(idsArr[i], namesArr[i]);
        String saved = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        java.util.List<String> ids = new java.util.ArrayList<>();
        if (!saved.isEmpty()) for (String s : saved.split(",")) if (!s.isEmpty()) ids.add(s);
        if (ids.isEmpty()) return "(none)";
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String id : ids) names.add(map.getOrDefault(id, id));
        if (names.size() == 1) return names.get(0);
        if (names.size() == 2) return names.get(0) + ", " + names.get(1);
        return names.get(0) + ", " + names.get(1) + " +" + (names.size() - 2);
    }

    private String positionName(int pos) {
        switch (pos) {
            case 0: return "verse";
            case 1: return "range";
            case 2: return "page";
            case 3: return "surah";
        }
        return String.valueOf(pos);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new android.content.Intent(this, com.repeatquran.settings.SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
