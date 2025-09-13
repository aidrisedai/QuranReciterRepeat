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
        findViewById(R.id.btnResume).setOnClickListener(v -> sendServiceAction(PlaybackService.ACTION_RESUME));

        // Default: enable Resume; receiver will disable when active playback is present
        android.view.View btnResume = findViewById(R.id.btnResume);
        btnResume.setEnabled(true);

        setupRepeatDropdown();

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
        });

        // Surah dropdown setup
        setupSurahDropdown();
        setupSingleSurahDropdown();
        findViewById(R.id.btnLoadSurah).setOnClickListener(v -> onLoadSurah());

        // Open Downloads screen
        findViewById(R.id.btnOpenDownloads).setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(this, com.repeatquran.downloads.DownloadsActivity.class);
            startActivity(i);
        });
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
        registerReceiver(playbackStateReceiver, f);
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
    @Override
    protected void onResume() {
        super.onResume();
        renderRecentHistory();
    }

    private void renderRecentHistory() {
        View container = findViewById(R.id.historyContainer);
        if (!(container instanceof android.widget.LinearLayout)) return;
        android.widget.LinearLayout ll = (android.widget.LinearLayout) container;
        ll.removeAllViews();
        // Load sessions off-main and then render
        new Thread(() -> {
            SessionRepository repo = new SessionRepository(this);
            java.util.List<SessionEntity> sessions = repo.getLastSessions(4);
            runOnUiThread(() -> {
                if (sessions == null || sessions.isEmpty()) {
                    android.widget.TextView tv = new android.widget.TextView(this);
                    tv.setText("No recent sessions");
                    ll.addView(tv);
                } else {
                    for (SessionEntity e : sessions) {
                        ll.addView(buildHistoryItemView(e));
                    }
                }
            });
        }).start();
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
    }
}
