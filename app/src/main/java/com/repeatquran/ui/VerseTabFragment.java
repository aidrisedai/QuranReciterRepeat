package com.repeatquran.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import com.repeatquran.R;
import com.repeatquran.playback.PlaybackService;

public class VerseTabFragment extends Fragment {
    private android.content.BroadcastReceiver playbackBr;

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
        AutoCompleteTextView ddAyah = root.findViewById(R.id.ayahDropdown);

        String[] display = com.repeatquran.util.SurahNames.displayList();
        ArrayAdapter<String> surahAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, display);
        ddSurah.setAdapter(surahAdapter);
        ddSurah.setThreshold(0);
        
        // Prefill last surah if available
        int lastSurah = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("last.surah.single", 1);
        if (lastSurah >= 1 && lastSurah <= 114) {
            ddSurah.setText(com.repeatquran.util.SurahNames.display(lastSurah), false);
            setupAyahDropdown(ddAyah, ayahLayout, lastSurah);
        }

        // When surah changes, update ayah dropdown
        ddSurah.setOnItemClickListener((p, v, pos, id) -> {
            int surahNumber = pos + 1;
            setupAyahDropdown(ddAyah, ayahLayout, surahNumber);
        });

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
            int ayah = parseIntSafe(ddAyah);
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
        playbackBr = new android.content.BroadcastReceiver() {
            @Override 
            public void onReceive(android.content.Context context, android.content.Intent intent) {
                android.view.View rootView = getView();
                if (rootView == null) return;
                boolean hasQueue = intent.getBooleanExtra("hasQueue", false);
                boolean playing = intent.getBooleanExtra("playing", false);
                android.view.View btn = rootView.findViewById(R.id.btnPause);
                if (btn instanceof com.google.android.material.button.MaterialButton) {
                    com.google.android.material.button.MaterialButton b = (com.google.android.material.button.MaterialButton) btn;
                    b.setText(playing ? "Pause" : "Resume");
                    b.setEnabled(hasQueue);
                }
            }
        };
        
        // Stop button
        root.findViewById(R.id.btnStop).setOnClickListener(v -> {
            sendService(PlaybackService.ACTION_STOP);
            android.widget.Toast.makeText(requireContext(), "Stopped", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override public void onStart() {
        super.onStart();
        if (playbackBr != null) {
            android.content.IntentFilter f = new android.content.IntentFilter(PlaybackService.ACTION_PLAYBACK_STATE);
            if (android.os.Build.VERSION.SDK_INT >= 33) requireContext().registerReceiver(playbackBr, f, android.content.Context.RECEIVER_NOT_EXPORTED); else requireContext().registerReceiver(playbackBr, f);
        }
    }

    @Override public void onStop() {
        super.onStop();
        if (playbackBr != null) {
            try { requireContext().unregisterReceiver(playbackBr); } catch (Exception ignored) {}
        }
    }

    private void sendService(String action) {
        Intent intent = new Intent(requireContext(), PlaybackService.class);
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= 26) requireContext().startForegroundService(intent); else requireContext().startService(intent);
    }

    private void showError(TextInputLayout layout, String msg) { layout.setError(msg); }
    private void clearError(TextInputLayout layout) { layout.setError(null); layout.setErrorEnabled(false); }
    private int parseIntSafe(AutoCompleteTextView edit) { try { return Integer.parseInt(edit.getText()==null?"":edit.getText().toString().trim()); } catch (Exception e) { return -1; } }
    
    private void setupAyahDropdown(AutoCompleteTextView ddAyah, TextInputLayout ayahLayout, int surahNumber) {
        int maxAyah = getAyahCount(surahNumber);
        
        // Create list of ayah numbers
        List<String> ayahNumbers = new ArrayList<>();
        for (int i = 1; i <= maxAyah; i++) {
            ayahNumbers.add(String.valueOf(i));
        }
        
        // Create adapter with filtering support
        ArrayAdapter<String> ayahAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, ayahNumbers) {
            @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        if (constraint == null || constraint.length() == 0) {
                            results.values = ayahNumbers;
                            results.count = ayahNumbers.size();
                        } else {
                            List<String> filtered = new ArrayList<>();
                            String filterString = constraint.toString().toLowerCase();
                            for (String ayah : ayahNumbers) {
                                if (ayah.startsWith(filterString)) {
                                    filtered.add(ayah);
                                }
                            }
                            results.values = filtered;
                            results.count = filtered.size();
                        }
                        return results;
                    }
                    
                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        if (results.values != null) {
                            clear();
                            addAll((List<String>) results.values);
                            notifyDataSetChanged();
                        }
                    }
                };
            }
        };
        
        ddAyah.setAdapter(ayahAdapter);
        ddAyah.setThreshold(1);
        
        // Set default to ayah 1
        ddAyah.setText("1", false);
        ayahLayout.setHelperText("Max ayah: " + maxAyah);
        
        // Clear any existing error
        clearError(ayahLayout);
        
        // Add real-time validation with red border for invalid input
        ddAyah.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if (input.isEmpty()) {
                    clearError(ayahLayout);
                    return;
                }
                
                try {
                    int ayahNum = Integer.parseInt(input);
                    if (ayahNum < 1 || ayahNum > maxAyah) {
                        // Show red border with max ayah in helper text
                        ayahLayout.setHelperText("Max ayah: " + maxAyah);
                        ayahLayout.setError(" "); // Space to trigger red border without message
                    } else {
                        // Valid input - clear error
                        clearError(ayahLayout);
                        ayahLayout.setHelperText("Max ayah: " + maxAyah);
                    }
                } catch (NumberFormatException e) {
                    // Invalid number format
                    ayahLayout.setHelperText("Max ayah: " + maxAyah);
                    ayahLayout.setError(" "); // Space to trigger red border without message
                }
            }
        });
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
