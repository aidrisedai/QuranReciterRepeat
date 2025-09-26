package com.repeatquran.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.PopupMenu;

import com.google.android.material.button.MaterialButton;
import com.repeatquran.playback.PlaybackService;

public class SpeedControlHelper {
    private static final float[] SPEED_VALUES = new float[]{0.5f,0.75f,1.0f,1.25f,1.5f,1.75f,2.0f};

    public static void setup(Context ctx, MaterialButton button) {
        if (button == null || ctx == null) return;
        float saved = ctx.getSharedPreferences("rq_prefs", Context.MODE_PRIVATE).getFloat("playback.speed", 1.0f);
        button.setText(format(saved));
        button.setOnClickListener(v -> showMenu(ctx, button));
    }

    private static void showMenu(Context ctx, MaterialButton anchor) {
        PopupMenu popup = new PopupMenu(ctx, anchor);
        for (int i = 0; i < SPEED_VALUES.length; i++) popup.getMenu().add(0, i, i, format(SPEED_VALUES[i]));
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            float v = SPEED_VALUES[id];
            ctx.getSharedPreferences("rq_prefs", Context.MODE_PRIVATE).edit().putFloat("playback.speed", v).apply();
            java.util.Map<String, Object> ev = new java.util.HashMap<>();
            ev.put("source", "tab_controls"); ev.put("speed", String.valueOf(v));
            com.repeatquran.analytics.AnalyticsLogger.get(ctx).log("speed_changed", ev);
            Intent i = new Intent(ctx, PlaybackService.class);
            i.setAction(PlaybackService.ACTION_SET_SPEED);
            i.putExtra("speed", v);
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i); else ctx.startService(i);
            anchor.setText(format(v));
            return true;
        });
        popup.show();
    }

    private static String format(float v) {
        if (Math.abs(v - 1.0f) < 0.001f) return "1.0×";
        return (v == (int)v ? String.format(java.util.Locale.US, "%d.0×", (int)v) : String.format(java.util.Locale.US, "%.2f×", v));
    }
}

