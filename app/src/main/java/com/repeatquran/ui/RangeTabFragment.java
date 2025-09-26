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

public class RangeTabFragment extends Fragment {
    private android.content.BroadcastReceiver playbackBr;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_range_tab, container, false);
        setupUi(v);
        return v;
    }

    private void setupUi(View root) {
        AutoCompleteTextView ddStart = root.findViewById(R.id.startSurahDropdown);
        AutoCompleteTextView ddEnd = root.findViewById(R.id.endSurahDropdown);
        TextInputLayout startSurahLayout = root.findViewById(R.id.startSurahLayout);
        TextInputLayout endSurahLayout = root.findViewById(R.id.endSurahLayout);
        TextInputLayout startAyahLayout = root.findViewById(R.id.startAyahLayout);
        TextInputLayout endAyahLayout = root.findViewById(R.id.endAyahLayout);
        TextInputEditText editStartAyah = root.findViewById(R.id.editStartAyah);
        TextInputEditText editEndAyah = root.findViewById(R.id.editEndAyah);

        String[] display = com.repeatquran.util.SurahNames.displayList();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, display);
        ddStart.setAdapter(adapter); ddStart.setThreshold(0);
        ddEnd.setAdapter(adapter); ddEnd.setThreshold(0);

        android.content.SharedPreferences prefsRange = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE);
        int lastStart = prefsRange.getInt("last.surah.range.start", 1);
        int lastEnd = prefsRange.getInt("last.surah.range.end", 1);
        if (lastStart>=1 && lastStart<=114) ddStart.setText(com.repeatquran.util.SurahNames.display(lastStart), false);
        if (lastEnd>=1 && lastEnd<=114) ddEnd.setText(com.repeatquran.util.SurahNames.display(lastEnd), false);
        int lastStartAyah = prefsRange.getInt("last.ayah.range.start", -1);
        int lastEndAyah = prefsRange.getInt("last.ayah.range.end", -1);
        if (lastStartAyah > 0) editStartAyah.setText(String.valueOf(lastStartAyah));
        if (lastEndAyah > 0) editEndAyah.setText(String.valueOf(lastEndAyah));

        startAyahLayout.setHelperText("Max ayah: " + com.repeatquran.util.AyahCounts.getCount(Math.max(1, Math.min(114, lastStart))));
        endAyahLayout.setHelperText("Max ayah: " + com.repeatquran.util.AyahCounts.getCount(Math.max(1, Math.min(114, lastEnd))));
        
        // Ensure all UI elements are visible initially
        ensureUIElementsVisible(root);
        
        ddStart.setOnItemClickListener((parent, v, pos, id) -> {
            startAyahLayout.setHelperText("Max ayah: " + com.repeatquran.util.AyahCounts.getCount(pos + 1));
            // Refresh UI visibility after dropdown interaction
            ensureUIElementsVisible(root);
        });
        
        ddEnd.setOnItemClickListener((parent, v, pos, id) -> {
            endAyahLayout.setHelperText("Max ayah: " + com.repeatquran.util.AyahCounts.getCount(pos + 1));
            // Refresh UI visibility after dropdown interaction
            ensureUIElementsVisible(root);
        });

        // Half-split now controlled via Settings only

        root.findViewById(R.id.btnPlay).setOnClickListener(v -> {
            // Guard against rapid clicks
            android.view.View btn = root.findViewById(R.id.btnPlay);
            if (!btn.isEnabled()) return;
            
            clearError(startSurahLayout); clearError(endSurahLayout); clearError(startAyahLayout); clearError(endAyahLayout);
            
            // Check for reciter selection before proceeding
            String savedOrder = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getString("reciters.order", "");
            if (savedOrder == null || savedOrder.trim().isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Select at least one reciter first", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            String s1 = ddStart.getText()!=null?ddStart.getText().toString().trim():"";
            String s2 = ddEnd.getText()!=null?ddEnd.getText().toString().trim():"";
            if (s1.length()<3) { showError(startSurahLayout, "Select start"); return; }
            if (s2.length()<3) { showError(endSurahLayout, "Select end"); return; }
            int ss, es;
            try { ss = Integer.parseInt(s1.substring(0,3)); } catch (Exception e) { showError(startSurahLayout, "Select start"); return; }
            try { es = Integer.parseInt(s2.substring(0,3)); } catch (Exception e) { showError(endSurahLayout, "Select end"); return; }
            if (ss<1||ss>114) { showError(startSurahLayout, "1..114"); return; }
            if (es<1||es>114) { showError(endSurahLayout, "1..114"); return; }
            int sa = parseIntSafe(editStartAyah);
            int ea = parseIntSafe(editEndAyah);
            if (sa<1||sa>getAyahCount(ss)) { showError(startAyahLayout, "Ayah 1.."+getAyahCount(ss)); return; }
            if (ea<1||ea>getAyahCount(es)) { showError(endAyahLayout, "Ayah 1.."+getAyahCount(es)); return; }
            if (!isStartBeforeOrEqual(ss, sa, es, ea)) { showError(endSurahLayout, "End before start"); showError(endAyahLayout, "End before start"); return; }

            requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).edit()
                    .putInt("last.surah.range.start", ss)
                    .putInt("last.surah.range.end", es)
                    .putInt("last.ayah.range.start", sa)
                    .putInt("last.ayah.range.end", ea)
                    .apply();

            int repeat = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("repeat.count", 1);
            boolean half = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getBoolean("ui.half.split", false);

            // Disable button immediately and show loading state  
            btn.setEnabled(false);
            android.widget.Toast.makeText(requireContext(), "Loading range…", android.widget.Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(requireContext(), PlaybackService.class);
            intent.setAction(PlaybackService.ACTION_LOAD_RANGE);
            intent.putExtra("ss", ss);
            intent.putExtra("sa", sa);
            intent.putExtra("es", es);
            intent.putExtra("ea", ea);
            intent.putExtra("repeat", repeat);
            intent.putExtra("halfSplit", half);
            
            if (Build.VERSION.SDK_INT >= 26) requireContext().startForegroundService(intent); else requireContext().startService(intent);
            
            // Re-enable after shorter delay, but service broadcast will manage state properly
            btn.postDelayed(() -> {
                if (btn.isEnabled() == false) { // Only re-enable if still disabled
                    btn.setEnabled(true);
                }
            }, 800);
        });

        root.findViewById(R.id.btnPause).setOnClickListener(v -> sendService(PlaybackService.ACTION_PAUSE));
        // Long-press Pause to Stop (clear queue and reset state)
        root.findViewById(R.id.btnPause).setOnLongClickListener(v -> {
            sendService(PlaybackService.ACTION_STOP);
            android.widget.Toast.makeText(requireContext(), "Stopped", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        });
        // Prepare receiver for pause/resume binding
        playbackBr = new android.content.BroadcastReceiver() {
            @Override 
            public void onReceive(android.content.Context context, android.content.Intent intent) {
                android.view.View rootView = getView();
                if (rootView == null) return;
                
                boolean hasQueue = intent.getBooleanExtra("hasQueue", false);
                boolean playing = intent.getBooleanExtra("playing", false);
                
                // Update Pause/Resume button
                android.view.View pauseBtn = rootView.findViewById(R.id.btnPause);
                if (pauseBtn instanceof com.google.android.material.button.MaterialButton) {
                    com.google.android.material.button.MaterialButton b = (com.google.android.material.button.MaterialButton) pauseBtn;
                    b.setText(playing ? "Pause" : "Resume");
                    b.setEnabled(hasQueue);
                }
                
                // Update Play button state - re-enable when service is ready
                android.view.View playBtn = rootView.findViewById(R.id.btnPlay);
                if (playBtn != null && hasQueue) {
                    // Service has successfully processed the load request
                    playBtn.setEnabled(true);
                }
                
                // Ensure UI elements remain visible after service state changes
                ensureUIElementsVisible(rootView);
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
    
    @Override public void onResume() {
        super.onResume();
        // Ensure UI elements are visible when fragment becomes active
        View rootView = getView();
        if (rootView != null) {
            ensureUIElementsVisible(rootView);
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

    private boolean isStartBeforeOrEqual(int ss, int sa, int es, int ea) {
        if (ss < es) return true; if (ss > es) return false; return sa <= ea;
    }
    private void showError(TextInputLayout layout, String msg) { layout.setError(msg); }
    private void clearError(TextInputLayout layout) { layout.setError(null); layout.setErrorEnabled(false); }
    private int parseIntSafe(TextInputEditText edit) { try { return Integer.parseInt(edit.getText()==null?"":edit.getText().toString().trim()); } catch (Exception e) { return -1; } }
    private int getAyahCount(int surah) { return com.repeatquran.util.AyahCounts.getCount(surah); }
    
    /**
     * Ensures all UI elements are properly visible and laid out.
     * This fixes the issue where End Ayah and Play buttons may not appear.
     */
    private void ensureUIElementsVisible(View root) {
        // Force visibility of all key UI elements
        View endAyahLayout = root.findViewById(R.id.endAyahLayout);
        View endSurahLayout = root.findViewById(R.id.endSurahLayout);
        View btnPlay = root.findViewById(R.id.btnPlay);
        View btnPause = root.findViewById(R.id.btnPause);
        View btnStop = root.findViewById(R.id.btnStop);
        
        if (endAyahLayout != null) {
            endAyahLayout.setVisibility(View.VISIBLE);
            // Force layout refresh
            endAyahLayout.requestLayout();
        }
        
        if (endSurahLayout != null) {
            endSurahLayout.setVisibility(View.VISIBLE);
            endSurahLayout.requestLayout();
        }
        
        if (btnPlay != null) {
            btnPlay.setVisibility(View.VISIBLE);
            btnPlay.requestLayout();
        }
        
        if (btnPause != null) {
            btnPause.setVisibility(View.VISIBLE);
            btnPause.requestLayout();
        }
        
        if (btnStop != null) {
            btnStop.setVisibility(View.VISIBLE);
            btnStop.requestLayout();
        }
        
        // Force the parent layout to refresh
        root.requestLayout();
        
        // Post a layout update to ensure proper rendering
        root.post(() -> {
            if (getView() != null) {
                getView().invalidate();
            }
        });
    }
}
