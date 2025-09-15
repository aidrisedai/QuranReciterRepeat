package com.repeatquran.downloads;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.repeatquran.R;
import com.repeatquran.data.CacheManager;
import com.repeatquran.data.db.PageSegmentDao;
import com.repeatquran.data.db.PageSegmentEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadsActivity extends AppCompatActivity {

    private AutoCompleteTextView reciterDropdown;
    private AutoCompleteTextView surahDropdown;
    private TextInputEditText pageEdit;
    private TextView surahStatus;
    private TextView pageStatus;
    private CacheManager cacheManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);
        com.repeatquran.analytics.AnalyticsLogger.get(this).log("downloads_opened", java.util.Collections.emptyMap());
        cacheManager = CacheManager.get(this);

        reciterDropdown = findViewById(R.id.reciterDropdown);
        surahDropdown = findViewById(R.id.surahDropdownDl);
        pageEdit = findViewById(R.id.editPageDl);
        surahStatus = findViewById(R.id.txtSurahStatus);
        pageStatus = findViewById(R.id.txtPageStatus);

        setupReciterDropdown();
        setupSurahDropdown();

        // Prefill from saved prefs where possible
        int lastPage = getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("last.page", 1);
        pageEdit.setText(String.valueOf(lastPage));

        findViewById(R.id.btnCheckSurah).setOnClickListener(v -> { updateSurahStatus(); });
        findViewById(R.id.btnDownloadSurah).setOnClickListener(v -> { downloadMissingForSurah(); com.repeatquran.analytics.AnalyticsLogger.get(this).log("downloads_surah_download", java.util.Collections.singletonMap("surah", String.valueOf(selectedSurah()))); });
        findViewById(R.id.btnClearSurah).setOnClickListener(v -> { clearSurahCache(); com.repeatquran.analytics.AnalyticsLogger.get(this).log("downloads_surah_clear", java.util.Collections.singletonMap("surah", String.valueOf(selectedSurah()))); });

        findViewById(R.id.btnCheckPage).setOnClickListener(v -> { updatePageStatus(); });
        findViewById(R.id.btnDownloadPage).setOnClickListener(v -> { downloadMissingForPage(); com.repeatquran.analytics.AnalyticsLogger.get(this).log("downloads_page_download", java.util.Collections.singletonMap("page", String.valueOf(parseIntSafe(pageEdit)))); });
        findViewById(R.id.btnClearPage).setOnClickListener(v -> { clearPageCache(); com.repeatquran.analytics.AnalyticsLogger.get(this).log("downloads_page_clear", java.util.Collections.singletonMap("page", String.valueOf(parseIntSafe(pageEdit)))); });
    }

    private void setupReciterDropdown() {
        String[] names = getResources().getStringArray(R.array.reciter_names);
        String[] ids = getResources().getStringArray(R.array.reciter_ids);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        reciterDropdown.setAdapter(adapter);
        // Set to first selected reciter from prefs if any
        String saved = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        String defaultId = ids[0];
        String defaultName = names[0];
        if (saved != null && !saved.isEmpty()) {
            String first = saved.split(",")[0];
            for (int i = 0; i < ids.length; i++) {
                if (ids[i].equals(first)) { defaultId = ids[i]; defaultName = names[i]; break; }
            }
        }
        reciterDropdown.setText(defaultName, false);
        reciterDropdown.setTag(defaultId);
        reciterDropdown.setOnItemClickListener((parent, view, position, id) -> reciterDropdown.setTag(ids[position]));
    }

    private void setupSurahDropdown() {
        String[] numbers = getResources().getStringArray(R.array.surah_numbers);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, numbers);
        surahDropdown.setAdapter(adapter);
        int lastSurah = getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("last.surah", 1);
        String lbl = String.format("%03d", Math.max(1, Math.min(114, lastSurah)));
        surahDropdown.setText(lbl, false);
    }

    private String selectedReciterId() {
        Object tag = reciterDropdown.getTag();
        if (tag instanceof String) return (String) tag;
        // resolve by name index
        String name = reciterDropdown.getText() != null ? reciterDropdown.getText().toString() : "";
        String[] names = getResources().getStringArray(R.array.reciter_names);
        String[] ids = getResources().getStringArray(R.array.reciter_ids);
        for (int i = 0; i < names.length; i++) if (names[i].equals(name)) return ids[i];
        return ids[0];
    }

    private int selectedSurah() {
        String txt = surahDropdown.getText() != null ? surahDropdown.getText().toString().trim() : "";
        try { return Integer.parseInt(txt); } catch (Exception e) { return 1; }
    }

    private void updateSurahStatus() {
        String rid = selectedReciterId();
        int surah = selectedSurah();
        int total = getAyahCount(surah);
        int cached = 0;
        String sss = String.format("%03d", surah);
        for (int a = 1; a <= total; a++) {
            String aaa = String.format("%03d", a);
            if (cacheManager.isCached(rid, sss, aaa)) cached++;
        }
        renderStatus(surahStatus, cached, total);
    }

    private void downloadMissingForSurah() {
        String rid = selectedReciterId();
        int surah = selectedSurah();
        int total = getAyahCount(surah);
        String sss = String.format("%03d", surah);
        for (int a = 1; a <= total; a++) {
            String aaa = String.format("%03d", a);
            if (!cacheManager.isCached(rid, sss, aaa)) {
                String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                cacheManager.cacheAsync(url, rid, sss, aaa);
            }
        }
        Toast.makeText(this, "Downloading missing ayahs for Surah " + sss, Toast.LENGTH_SHORT).show();
        // Update status shortly after enqueuing
        ((MaterialButton)findViewById(R.id.btnCheckSurah)).postDelayed(this::updateSurahStatus, 1000);
    }

    private void clearSurahCache() {
        String rid = selectedReciterId();
        int surah = selectedSurah();
        int total = getAyahCount(surah);
        String sss = String.format("%03d", surah);
        int removed = 0;
        for (int a = 1; a <= total; a++) {
            String aaa = String.format("%03d", a);
            File f = cacheManager.getTargetFile(rid, sss, aaa);
            if (f.exists() && f.delete()) removed++;
        }
        Toast.makeText(this, "Removed " + removed + " files for Surah " + sss, Toast.LENGTH_SHORT).show();
        updateSurahStatus();
    }

    private void updatePageStatus() {
        String rid = selectedReciterId();
        int page = parseIntSafe(pageEdit);
        if (page < 1 || page > 604) { pageStatus.setText("Enter 1–604"); return; }
        List<int[]> ayahs = ayahsForPage(page);
        int total = ayahs.size();
        int cached = 0;
        for (int[] pair : ayahs) {
            String sss = String.format("%03d", pair[0]);
            String aaa = String.format("%03d", pair[1]);
            if (cacheManager.isCached(rid, sss, aaa)) cached++;
        }
        renderStatus(pageStatus, cached, total);
    }

    private void downloadMissingForPage() {
        String rid = selectedReciterId();
        int page = parseIntSafe(pageEdit);
        if (page < 1 || page > 604) { Toast.makeText(this, "Enter 1–604", Toast.LENGTH_SHORT).show(); return; }
        List<int[]> ayahs = ayahsForPage(page);
        for (int[] pair : ayahs) {
            String sss = String.format("%03d", pair[0]);
            String aaa = String.format("%03d", pair[1]);
            if (!cacheManager.isCached(rid, sss, aaa)) {
                String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                cacheManager.cacheAsync(url, rid, sss, aaa);
            }
        }
        Toast.makeText(this, "Downloading missing ayahs for Page " + page, Toast.LENGTH_SHORT).show();
        ((MaterialButton)findViewById(R.id.btnCheckPage)).postDelayed(this::updatePageStatus, 1000);
    }

    private void clearPageCache() {
        String rid = selectedReciterId();
        int page = parseIntSafe(pageEdit);
        if (page < 1 || page > 604) { Toast.makeText(this, "Enter 1–604", Toast.LENGTH_SHORT).show(); return; }
        List<int[]> ayahs = ayahsForPage(page);
        int removed = 0;
        for (int[] pair : ayahs) {
            String sss = String.format("%03d", pair[0]);
            String aaa = String.format("%03d", pair[1]);
            File f = cacheManager.getTargetFile(rid, sss, aaa);
            if (f.exists() && f.delete()) removed++;
        }
        Toast.makeText(this, "Removed " + removed + " files for Page " + page, Toast.LENGTH_SHORT).show();
        updatePageStatus();
    }

    private void renderStatus(TextView view, int cached, int total) {
        String icon = cached >= total && total > 0 ? "✅" : "⬇️";
        view.setText(icon + "  " + cached + "/" + total + " cached");
    }

    private int parseIntSafe(TextInputEditText edit) {
        try { return Integer.parseInt(edit.getText()==null?"":edit.getText().toString().trim()); } catch (Exception e) { return -1; }
    }

    private List<int[]> ayahsForPage(int page) {
        PageSegmentDao dao = RepeatQuranDatabase.get(this).pageSegmentDao();
        List<PageSegmentEntity> segs = dao.segmentsForPage(page);
        List<int[]> list = new ArrayList<>();
        for (PageSegmentEntity s : segs) {
            for (int a = s.startAyah; a <= s.endAyah; a++) list.add(new int[]{s.surah, a});
        }
        return list;
    }

    private int getAyahCount(int surah) {
        final int[] AYAH_COUNTS = new int[] {
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
        return AYAH_COUNTS[surah - 1];
    }
}
