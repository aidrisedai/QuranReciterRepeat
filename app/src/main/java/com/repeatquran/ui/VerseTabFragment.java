package com.repeatquran.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.repeatquran.R;
import com.repeatquran.playback.PlaybackService;

public class VerseTabFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_verse_tab, container, false);
        setupUi(v);
        return v;
    }

    private void setupUi(View root) {
        AutoCompleteTextView ddSurah = root.findViewById(R.id.surahDropdown);
        TextInputLayout surahLayout = root.findViewById(R.id.surahInputLayout);
        TextInputLayout ayahLayout = root.findViewById(R.id.ayahInputLayout);
        TextInputEditText editAyah = root.findViewById(R.id.editAyah);

        String[] display = com.repeatquran.util.SurahNames.displayList();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, display);
        ddSurah.setAdapter(adapter);
        ddSurah.setThreshold(0);
        // Prefill last surah if available
        int last = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("last.surah.single", 1);
        if (last >= 1 && last <= 114) {
            ddSurah.setText(com.repeatquran.util.SurahNames.display(last), false);
            // Show helper for max ayah on initial render
            ayahLayout.setHelperText("Max ayah: " + getAyahCount(last));
        }

        ddSurah.setOnItemClickListener((p, v, pos, id) -> ayahLayout.setHelperText("Max ayah: " + getAyahCount(pos + 1)));

        root.findViewById(R.id.btnPlay).setOnClickListener(v -> {
            // UI guard: require at least one reciter selected
            String savedOrder = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getString("reciters.order", "");
            if (savedOrder == null || savedOrder.trim().isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Select at least one reciter", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            clearError(surahLayout); clearError(ayahLayout);
            String txt = ddSurah.getText() != null ? ddSurah.getText().toString().trim() : "";
            if (txt.length() < 3) { showError(surahLayout, "Select surah"); return; }
            int surah;
            try { surah = Integer.parseInt(txt.substring(0,3)); } catch (Exception e) { showError(surahLayout, "Select surah"); return; }
            if (surah < 1 || surah > 114) { showError(surahLayout, "1..114"); return; }
            int ayah = parseIntSafe(editAyah);
            if (ayah < 1 || ayah > getAyahCount(surah)) { showError(ayahLayout, "Ayah 1.." + getAyahCount(surah)); return; }

            // Persist last selected
            requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).edit()
                    .putInt("last.surah.single", surah).apply();

            // Repeat comes from prefs (set on Home controls); pass through
            int repeat = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("repeat.count", 1);

            Intent intent = new Intent(requireContext(), PlaybackService.class);
            intent.setAction(PlaybackService.ACTION_LOAD_SINGLE);
            intent.putExtra("sura", surah);
            intent.putExtra("ayah", ayah);
            intent.putExtra("repeat", repeat);
            if (Build.VERSION.SDK_INT >= 26) requireContext().startForegroundService(intent); else requireContext().startService(intent);
            android.widget.Toast.makeText(requireContext(),
                    "Loading Surah " + String.format("%03d", surah) + " — " + com.repeatquran.util.SurahNames.name(surah) +
                            ", Ayah " + ayah + " (repeat=" + (repeat==-1?"∞":repeat) + ")",
                    android.widget.Toast.LENGTH_SHORT).show();
            View btn = root.findViewById(R.id.btnPlay);
            btn.setEnabled(false);
            btn.postDelayed(() -> btn.setEnabled(true), 1200);
        });

        root.findViewById(R.id.btnPause).setOnClickListener(v -> sendService(PlaybackService.ACTION_PAUSE));
        root.findViewById(R.id.btnPause).setOnLongClickListener(v -> {
            sendService(PlaybackService.ACTION_STOP);
            android.widget.Toast.makeText(requireContext(), "Stopped", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        });
        android.content.BroadcastReceiver br = new android.content.BroadcastReceiver() {
            @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
                boolean hasQueue = intent.getBooleanExtra("hasQueue", false);
                boolean playing = intent.getBooleanExtra("playing", false);
                android.view.View btn = root.findViewById(R.id.btnPause);
                if (btn instanceof com.google.android.material.button.MaterialButton) {
                    com.google.android.material.button.MaterialButton b = (com.google.android.material.button.MaterialButton) btn;
                    b.setText(playing ? "Pause" : "Resume");
                    b.setEnabled(hasQueue);
                }
            }
        };
        androidx.lifecycle.LifecycleOwner owner = getViewLifecycleOwner();
        owner.getLifecycle().addObserver(new androidx.lifecycle.DefaultLifecycleObserver() {
            @Override public void onStart(@androidx.annotation.NonNull androidx.lifecycle.LifecycleOwner owner) {
                android.content.IntentFilter f = new android.content.IntentFilter(PlaybackService.ACTION_PLAYBACK_STATE);
                if (android.os.Build.VERSION.SDK_INT >= 33) requireContext().registerReceiver(br, f, android.content.Context.RECEIVER_NOT_EXPORTED); else requireContext().registerReceiver(br, f);
            }
            @Override public void onStop(@androidx.annotation.NonNull androidx.lifecycle.LifecycleOwner owner) {
                try { requireContext().unregisterReceiver(br); } catch (Exception ignored) {}
            }
        });
        // Speed next to Play/Pause
        com.google.android.material.button.MaterialButton btnSpeed = root.findViewById(R.id.btnSpeed);
        com.repeatquran.ui.SpeedControlHelper.setup(requireContext(), btnSpeed);
    }

    private void sendService(String action) {
        Intent intent = new Intent(requireContext(), PlaybackService.class);
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= 26) requireContext().startForegroundService(intent); else requireContext().startService(intent);
    }

    private void showError(TextInputLayout layout, String msg) { layout.setError(msg); }
    private void clearError(TextInputLayout layout) { layout.setError(null); layout.setErrorEnabled(false); }
    private int parseIntSafe(TextInputEditText edit) { try { return Integer.parseInt(edit.getText()==null?"":edit.getText().toString().trim()); } catch (Exception e) { return -1; } }

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
