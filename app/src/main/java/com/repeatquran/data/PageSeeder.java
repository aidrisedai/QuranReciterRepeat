package com.repeatquran.data;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.repeatquran.data.db.PageSegmentDao;
import com.repeatquran.data.db.PageSegmentEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PageSeeder {
    private static final String TAG = "PageSeeder";

    public static void seedIfNeeded(Context context) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.execute(() -> {
            try {
                PageSegmentDao dao = RepeatQuranDatabase.get(context).pageSegmentDao();
                if (dao.countAll() > 0) return;
                List<PageSegmentEntity> all = loadFromAssets(context.getAssets(), "pages.json");
                dao.insertAll(all);
                Log.d(TAG, "Seeded page segments: " + all.size());
            } catch (Exception e) {
                Log.e(TAG, "Failed to seed page segments", e);
            }
        });
    }

    private static List<PageSegmentEntity> loadFromAssets(AssetManager am, String assetPath) throws Exception {
        try (InputStream is = am.open(assetPath);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JSONObject root = new JSONObject(sb.toString());
            JSONArray pages = root.getJSONArray("pages");
            List<PageSegmentEntity> out = new ArrayList<>();
            for (int i = 0; i < pages.length(); i++) {
                JSONObject p = pages.getJSONObject(i);
                int page = p.getInt("page");
                JSONArray segs = p.getJSONArray("segments");
                for (int j = 0; j < segs.length(); j++) {
                    JSONObject s = segs.getJSONObject(j);
                    PageSegmentEntity e = new PageSegmentEntity();
                    e.page = page;
                    e.orderIndex = j;
                    e.surah = s.getInt("surah");
                    e.startAyah = s.getInt("startAyah");
                    e.endAyah = s.getInt("endAyah");
                    out.add(e);
                }
            }
            return out;
        }
    }
}

