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

        // Warm up playback service so first Play doesn't pay cold-start cost
        sendServiceAction(com.repeatquran.playback.PlaybackService.ACTION_START);

        // Toolbar menu handling (Remember my mode, Settings)
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
                } else if (mi.getItemId() == R.id.action_stop) {
                    Intent i = new Intent(this, PlaybackService.class);
                    i.setAction(PlaybackService.ACTION_STOP);
                    if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
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
                                case 0: tab.setText("VERSE"); break;
                                case 1: tab.setText("RANGE"); break;
                                case 2: tab.setText("PAGE"); break;
                                case 3: tab.setText("SURAH"); break;
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
        // Inline repeat control (dropdown + editable number)
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

        // Inline controls row removed; real controls live inside tab fragments

        setupRepeatDropdown();
        setupSpeedDropdown();
        // Analytics: app open
        com.repeatquran.analytics.AnalyticsLogger.get(this).log("app_open", java.util.Collections.emptyMap());
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
            if (PlaybackService.ACTION_START.equals(action)) {
                // Warmup doesn't need foreground; avoid startForeground timeout
                startService(intent);
            } else {
                startForegroundService(intent);
            }
        } else {
            startService(intent);
        }
    }

    // No toolbar toggle updater

    private void setupRepeatDropdown() {
        AutoCompleteTextView dropdown = findViewById(R.id.repeatInlineDropdown);
        TextInputLayout inputLayout = findViewById(R.id.repeatInlineLayout);
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
            dropdown.clearFocus();
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

    // Speed controls in Home are provided via the inline dropdown near Reciters.

    private void setupSpeedDropdown() {
        AutoCompleteTextView dd = findViewById(R.id.speedInlineDropdown);
        if (dd == null) return;
        String[] labels = new String[]{"0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×"};
        final float[] values = new float[]{0.5f,0.75f,1.0f,1.25f,1.5f,1.75f,2.0f};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        dd.setAdapter(adapter);
        float saved = getSharedPreferences("rq_prefs", MODE_PRIVATE).getFloat("playback.speed", 1.0f);
        int sel = 2; float min = Float.MAX_VALUE;
        for (int i = 0; i < values.length; i++) { float d = Math.abs(values[i]-saved); if (d < min) { min = d; sel = i; } }
        dd.setText(labels[sel], false);
        dd.setOnItemClickListener((parent, view, position, id) -> {
            float v = values[position];
            getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putFloat("playback.speed", v).apply();
            java.util.Map<String, Object> ev = new java.util.HashMap<>();
            ev.put("source", "home"); ev.put("speed", String.valueOf(v));
            com.repeatquran.analytics.AnalyticsLogger.get(this).log("speed_changed", ev);
            Intent i = new Intent(this, PlaybackService.class);
            i.setAction(PlaybackService.ACTION_SET_SPEED);
            i.putExtra("speed", v);
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playbackStateReceiver == null) {
            playbackStateReceiver = new android.content.BroadcastReceiver() {
                @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
                    // No toolbar toggle to update; state is consumed elsewhere if needed
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

    // ---- Quick History UI (last 4 sessions) ----

    private void renderRecentHistory() {
        View container = findViewById(R.id.historyContainer);
        if (!(container instanceof android.widget.LinearLayout)) return;
        android.widget.LinearLayout ll = (android.widget.LinearLayout) container;
        ll.removeAllViews();
        // Load sessions off-main and then render
        new Thread(() -> {
            SessionRepository repo = new SessionRepository(this);
            java.util.List<SessionEntity> latest = repo.getLastSessions(6); // Reduced from 10 to 6 to save space
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
                if (distinct.size() == 2) break; // Keep maximum of 2 items to prevent overflow
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

    // Presets functionality is now handled in Settings

    // Preset UI is now handled in Settings

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

    // Preset editing is now handled in Settings

    // Preset deletion is now handled in Settings

    // Preset saving is now handled in Settings

    // Preset saving is now handled in Settings

    private View buildHistoryItemView(SessionEntity e) {
        android.widget.LinearLayout item = new android.widget.LinearLayout(this);
        item.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (12 * getResources().getDisplayMetrics().density); // Reduced from 16 to 12
        item.setPadding(pad, pad, pad, pad);
        
        // Create a more Material Design card-like appearance
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(0xF8F8F8); // Slightly lighter gray
        background.setCornerRadius(6 * getResources().getDisplayMetrics().density); // Reduced from 8 to 6
        item.setBackground(background);
        
        // Add smaller margin between items
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, (int)(6 * getResources().getDisplayMetrics().density)); // Reduced from 8 to 6
        item.setLayoutParams(params);

        String title;
        if ("single".equals(e.sourceType)) {
            String sss = String.format("%03d", e.startSurah);
            title = "Single | " + sss + " — " + surahName(e.startSurah) + " : " + e.startAyah;
        } else if ("range".equals(e.sourceType)) {
            String s1 = String.format("%03d", e.startSurah);
            String s2 = String.format("%03d", e.endSurah);
            title = "Range | " + s1 + " — " + surahName(e.startSurah) + ":" + e.startAyah +
                    " → " + s2 + " — " + surahName(e.endSurah) + ":" + e.endAyah;
        } else if ("page".equals(e.sourceType)) {
            title = "Page | (number not stored)";
        } else if ("surah".equals(e.sourceType)) {
            String sss = String.format("%03d", e.startSurah != null ? e.startSurah : 0);
            title = "Surah | " + sss + " — " + surahName(e.startSurah != null ? e.startSurah : 1);
        } else {
            title = "Provider";
        }

        android.widget.TextView tvTitle = new android.widget.TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(14); // Reduced from 16 to 14
        tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(0xFF212121);
        tvTitle.setMaxLines(1);
        tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        item.addView(tvTitle);

        // Reciters and repeat info
        String recSummary = recitersSummary(e.recitersCsv);
        android.widget.TextView tvSub = new android.widget.TextView(this);
        tvSub.setText("Reciters: " + recSummary + " | Repeat: " + (e.repeatCount == -1 ? "∞" : e.repeatCount));
        tvSub.setTextSize(12); // Reduced from 14 to 12
        tvSub.setTextColor(0xFF757575);
        tvSub.setPadding(0, (int)(2 * getResources().getDisplayMetrics().density), 0, 0); // Reduced from 4 to 2
        tvSub.setMaxLines(1);
        tvSub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        item.addView(tvSub);

        // Click to replay where possible
        if (!"provider".equals(e.sourceType)) {
            item.setOnClickListener(v -> replaySession(e));
            // Add ripple effect for better feedback
            android.graphics.drawable.RippleDrawable ripple = new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x1A000000),
                background,
                null
            );
            item.setBackground(ripple);
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


    // ---- Reciter multi-select ----
    private void showReciterPicker() {
        showTabbedReciterPicker();
    }
    
    private void showTabbedReciterPicker() {
        SharedPreferences prefs = getSharedPreferences("rq_prefs", MODE_PRIVATE);
        String saved = prefs.getString("reciters.order", "");
        java.util.List<String> currentSelection = new java.util.ArrayList<>();
        if (!saved.isEmpty()) {
            for (String s : saved.split(",")) if (!s.isEmpty()) currentSelection.add(s);
        }
        
        // Get reciter data
        String[] originalNames = getResources().getStringArray(R.array.reciter_names);
        String[] originalIds = getResources().getStringArray(R.array.reciter_ids);
        
        // Create pairs to maintain name-id relationship while sorting
        java.util.List<java.util.AbstractMap.SimpleEntry<String, String>> reciterPairs = new java.util.ArrayList<>();
        for (int i = 0; i < originalNames.length; i++) {
            reciterPairs.add(new java.util.AbstractMap.SimpleEntry<>(originalNames[i], originalIds[i]));
        }
        
        // Sort pairs alphabetically by name (case-insensitive)
        reciterPairs.sort((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()));
        
        // Extract sorted names and ids
        String[] names = new String[reciterPairs.size()];
        String[] ids = new String[reciterPairs.size()];
        for (int i = 0; i < reciterPairs.size(); i++) {
            names[i] = reciterPairs.get(i).getKey();
            ids[i] = reciterPairs.get(i).getValue();
        }
        
        // Create boolean array for current selections
        boolean[] checkedItems = new boolean[names.length];
        for (int i = 0; i < ids.length; i++) {
            checkedItems[i] = currentSelection.contains(ids[i]);
        }
        
        // Create simple multi-choice dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Reciters")
            .setMultiChoiceItems(names, checkedItems, (dialog, which, isChecked) -> {
                // Handle individual item clicks
                checkedItems[which] = isChecked;
            })
            .setPositiveButton("OK", (dialog, which) -> {
                // Save selections
                java.util.List<String> selectedIds = new java.util.ArrayList<>();
                for (int i = 0; i < checkedItems.length; i++) {
                    if (checkedItems[i]) {
                        selectedIds.add(ids[i]);
                    }
                }
                String joined = android.text.TextUtils.join(",", selectedIds);
                prefs.edit().putString("reciters.order", joined).apply();
                renderSelectedReciters();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    

    private void renderSelectedReciters() {
        // Update the global pills to show reciter count
        refreshGlobalPills();
    }

    private void refreshGlobalPills() {
        android.view.View v = findViewById(R.id.chipReciters);
        if (v instanceof com.google.android.material.chip.Chip) {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) v;
            chip.setText("Reciters: " + summarizeReciters());
        }
    }

    private String summarizeReciters() {
        String saved = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        java.util.List<String> ids = new java.util.ArrayList<>();
        if (!saved.isEmpty()) for (String s : saved.split(",")) if (!s.isEmpty()) ids.add(s);
        if (ids.isEmpty()) return "0 selected";
        return ids.size() + " selected";
    }


    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new android.content.Intent(this, com.repeatquran.settings.SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
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
}
