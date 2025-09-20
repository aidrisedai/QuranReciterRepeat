package com.repeatquran.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.repeatquran.R;

public class SettingsActivity extends AppCompatActivity {
    private android.widget.AutoCompleteTextView reciterDropdown;
    private android.widget.AutoCompleteTextView surahDropdown;
    private com.google.android.material.textfield.TextInputEditText pageEdit;
    private android.widget.TextView surahStatus;
    private android.widget.TextView pageStatus;
    private com.repeatquran.data.CacheManager cacheManager;
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

        // Downloads (merged)
        cacheManager = com.repeatquran.data.CacheManager.get(this);
        reciterDropdown = findViewById(R.id.reciterDropdown);
        surahDropdown = findViewById(R.id.surahDropdownDl);
        pageEdit = findViewById(R.id.editPageDl);
        surahStatus = findViewById(R.id.txtSurahStatus);
        pageStatus = findViewById(R.id.txtPageStatus);

        setupReciterDropdown();
        setupSurahDropdown();

        int lastPage = getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("last.page", 1);
        if (pageEdit != null) pageEdit.setText(String.valueOf(lastPage));

        findViewById(R.id.btnCheckSurah).setOnClickListener(v -> updateSurahStatus());
        findViewById(R.id.btnDownloadSurah).setOnClickListener(v -> { downloadMissingForSurah(); com.repeatquran.analytics.AnalyticsLogger.get(this).log("downloads_surah_download", java.util.Collections.singletonMap("surah", String.valueOf(selectedSurah()))); });
        findViewById(R.id.btnClearSurah).setOnClickListener(v -> { clearSurahCache(); com.repeatquran.analytics.AnalyticsLogger.get(this).log("downloads_surah_clear", java.util.Collections.singletonMap("surah", String.valueOf(selectedSurah()))); });

        findViewById(R.id.btnCheckPage).setOnClickListener(v -> updatePageStatus());
        findViewById(R.id.btnDownloadPage).setOnClickListener(v -> { downloadMissingForPage(); com.repeatquran.analytics.AnalyticsLogger.get(this).log("downloads_page_download", java.util.Collections.singletonMap("page", String.valueOf(parseIntSafe(pageEdit)))); });
        findViewById(R.id.btnClearPage).setOnClickListener(v -> { clearPageCache(); com.repeatquran.analytics.AnalyticsLogger.get(this).log("downloads_page_clear", java.util.Collections.singletonMap("page", String.valueOf(parseIntSafe(pageEdit)))); });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---- Downloads merged helpers ----

private void setupReciterDropdown() {
    String[] names = getResources().getStringArray(R.array.reciter_names);
    String[] ids = getResources().getStringArray(R.array.reciter_ids);
    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
    reciterDropdown.setAdapter(adapter);
    String saved = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
    String defaultId = ids[0];
    String defaultName = names[0];
    if (saved != null && !saved.isEmpty()) {
        String first = saved.split(",")[0];
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(first)) { defaultId = ids[i]; defaultName = names[i]; break; }
    }
    reciterDropdown.setText(defaultName, false);
    reciterDropdown.setTag(defaultId);
    reciterDropdown.setOnItemClickListener((parent, view, position, id) -> reciterDropdown.setTag(ids[position]));
}

private void setupSurahDropdown() {
    String[] display = com.repeatquran.util.SurahNames.displayList();
    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, display);
    surahDropdown.setAdapter(adapter);
    int lastSurah = getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("last.surah", 1);
    int bounded = Math.max(1, Math.min(114, lastSurah));
    surahDropdown.setText(com.repeatquran.util.SurahNames.display(bounded), false);
}

// Speed control moved to controls row in Home

private String selectedReciterId() {
    Object tag = reciterDropdown.getTag();
    if (tag instanceof String) return (String) tag;
    String name = reciterDropdown.getText() != null ? reciterDropdown.getText().toString() : "";
    String[] names = getResources().getStringArray(R.array.reciter_names);
    String[] ids = getResources().getStringArray(R.array.reciter_ids);
    for (int i = 0; i < names.length; i++) if (names[i].equals(name)) return ids[i];
    return ids[0];
}

private int selectedSurah() {
    String txt = surahDropdown.getText() != null ? surahDropdown.getText().toString().trim() : "";
    if (txt.length() >= 3) {
        try { return Integer.parseInt(txt.substring(0, 3)); } catch (Exception ignored) {}
    }
    return 1;
}

private void updateSurahStatus() {
    final String rid = selectedReciterId();
    final int surah = selectedSurah();
    new Thread(() -> {
        int total = getAyahCount(surah);
        int cached = 0;
        String sss = String.format("%03d", surah);
        for (int a = 1; a <= total; a++) {
            String aaa = String.format("%03d", a);
            if (cacheManager.isCached(rid, sss, aaa)) cached++;
        }
        final int fCached = cached, fTotal = total;
        runOnUiThread(() -> renderStatus(surahStatus, fCached, fTotal));
    }).start();
}

private void downloadMissingForSurah() {
    final String rid = selectedReciterId();
    final int surah = selectedSurah();
    final String sss = String.format("%03d", surah);
    setSurahButtonsEnabled(true);
    new Thread(() -> {
        int total = getAyahCount(surah);
        for (int a = 1; a <= total; a++) {
            String aaa = String.format("%03d", a);
            if (!cacheManager.isCached(rid, sss, aaa)) {
                String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                cacheManager.cacheAsync(url, rid, sss, aaa);
            }
        }
        runOnUiThread(() -> {
            android.widget.Toast.makeText(this, "Downloading missing ayahs for Surah " + sss, android.widget.Toast.LENGTH_SHORT).show();
            updateSurahStatus();
            setSurahButtonsEnabled(true);
        });
    }).start();
}

private void clearSurahCache() {
    final String rid = selectedReciterId();
    final int surah = selectedSurah();
    final String sss = String.format("%03d", surah);
    setSurahButtonsEnabled(false);
    new Thread(() -> {
        int total = getAyahCount(surah);
        int removed = 0;
        for (int a = 1; a <= total; a++) {
            String aaa = String.format("%03d", a);
            java.io.File f = cacheManager.getTargetFile(rid, sss, aaa);
            if (f.exists() && f.delete()) removed++;
        }
        final int removedFinal = removed;
        runOnUiThread(() -> {
            android.widget.Toast.makeText(this, "Removed " + removedFinal + " files for Surah " + sss, android.widget.Toast.LENGTH_SHORT).show();
            updateSurahStatus();
            setSurahButtonsEnabled(true);
        });
    }).start();
}

private void updatePageStatus() {
    final String rid = selectedReciterId();
    final int page = parseIntSafe(pageEdit);
    if (page < 1 || page > 604) { pageStatus.setText("Enter 1–604"); return; }
    new Thread(() -> {
        java.util.List<int[]> ayahs = ayahsForPage(page);
        int total = ayahs.size();
        int cached = 0;
        for (int[] pair : ayahs) {
            String sss = String.format("%03d", pair[0]);
            String aaa = String.format("%03d", pair[1]);
            if (cacheManager.isCached(rid, sss, aaa)) cached++;
        }
        final int fCached = cached, fTotal = total;
        runOnUiThread(() -> renderStatus(pageStatus, fCached, fTotal));
    }).start();
}

private void downloadMissingForPage() {
    final String rid = selectedReciterId();
    final int page = parseIntSafe(pageEdit);
    if (page < 1 || page > 604) { android.widget.Toast.makeText(this, "Enter 1–604", android.widget.Toast.LENGTH_SHORT).show(); return; }
    setPageButtonsEnabled(false);
    new Thread(() -> {
        java.util.List<int[]> ayahs = ayahsForPage(page);
        for (int[] pair : ayahs) {
            String sss = String.format("%03d", pair[0]);
            String aaa = String.format("%03d", pair[1]);
            if (!cacheManager.isCached(rid, sss, aaa)) {
                String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                cacheManager.cacheAsync(url, rid, sss, aaa);
            }
        }
        runOnUiThread(() -> {
            android.widget.Toast.makeText(this, "Downloading missing ayahs for Page " + page, android.widget.Toast.LENGTH_SHORT).show();
            updatePageStatus();
            setPageButtonsEnabled(true);
        });
    }).start();
}

private void clearPageCache() {
    final String rid = selectedReciterId();
    final int page = parseIntSafe(pageEdit);
    if (page < 1 || page > 604) { android.widget.Toast.makeText(this, "Enter 1–604", android.widget.Toast.LENGTH_SHORT).show(); return; }
    setPageButtonsEnabled(false);
    new Thread(() -> {
        java.util.List<int[]> ayahs = ayahsForPage(page);
        int removed = 0;
        for (int[] pair : ayahs) {
            String sss = String.format("%03d", pair[0]);
            String aaa = String.format("%03d", pair[1]);
            java.io.File f = cacheManager.getTargetFile(rid, sss, aaa);
            if (f.exists() && f.delete()) removed++;
        }
        final int removedFinal = removed;
        runOnUiThread(() -> {
            android.widget.Toast.makeText(this, "Removed " + removedFinal + " files for Page " + page, android.widget.Toast.LENGTH_SHORT).show();
            updatePageStatus();
            setPageButtonsEnabled(true);
        });
    }).start();
}

private void setSurahButtonsEnabled(boolean enabled) {
    findViewById(R.id.btnCheckSurah).setEnabled(enabled);
    findViewById(R.id.btnDownloadSurah).setEnabled(enabled);
    findViewById(R.id.btnClearSurah).setEnabled(enabled);
}

private void setPageButtonsEnabled(boolean enabled) {
    findViewById(R.id.btnCheckPage).setEnabled(enabled);
    findViewById(R.id.btnDownloadPage).setEnabled(enabled);
    findViewById(R.id.btnClearPage).setEnabled(enabled);
}

private void renderStatus(android.widget.TextView view, int cached, int total) {
    String icon = cached >= total && total > 0 ? "✅" : "⬇️";
    view.setText(icon + "  " + cached + "/" + total + " cached");
}

private int parseIntSafe(com.google.android.material.textfield.TextInputEditText edit) {
    try { return Integer.parseInt(edit.getText()==null?"":edit.getText().toString().trim()); } catch (Exception e) { return -1; }
}

private java.util.List<int[]> ayahsForPage(int page) {
    com.repeatquran.data.db.PageSegmentDao dao = com.repeatquran.data.db.RepeatQuranDatabase.get(this).pageSegmentDao();
    java.util.List<com.repeatquran.data.db.PageSegmentEntity> segs = dao.segmentsForPage(page);
    java.util.List<int[]> list = new java.util.ArrayList<>();
    for (com.repeatquran.data.db.PageSegmentEntity s : segs) {
        for (int a = s.startAyah; a <= s.endAyah; a++) list.add(new int[]{s.surah, a});
    }
    return list;
}

private int getAyahCount(int surah) { return com.repeatquran.util.AyahCounts.getCount(surah); }

}
