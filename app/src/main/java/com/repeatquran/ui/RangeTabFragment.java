package com.repeatquran.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;
import com.repeatquran.R;
import com.repeatquran.playback.PlaybackService;

public class RangeTabFragment extends BaseTabFragment {
    private int lastStartSurah = -1; // Track previous start surah for smart auto-sync
    
    // UI references for state persistence
    private AutoCompleteTextView ddStartSurah;
    private AutoCompleteTextView ddEndSurah;
    private AutoCompleteTextView ddStartAyah;
    private AutoCompleteTextView ddEndAyah;
    
    @Override
    protected String getFragmentTag() {
        return "RangeTabFragment";
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_range_tab, container, false);
        setupUi(v);
        setupCommonButtons(v);
        return v;
    }

    private void setupUi(View root) {
        ddStartSurah = root.findViewById(R.id.startSurahDropdown);
        ddEndSurah = root.findViewById(R.id.endSurahDropdown);
        TextInputLayout startSurahLayout = root.findViewById(R.id.startSurahLayout);
        TextInputLayout endSurahLayout = root.findViewById(R.id.endSurahLayout);
        TextInputLayout startAyahLayout = root.findViewById(R.id.startAyahLayout);
        TextInputLayout endAyahLayout = root.findViewById(R.id.endAyahLayout);
        ddStartAyah = root.findViewById(R.id.startAyahDropdown);
        ddEndAyah = root.findViewById(R.id.endAyahDropdown);

        String[] display = com.repeatquran.util.SurahNames.displayList();
        
        // Search-as-you-type adapter for Surah selection
        com.repeatquran.ui.adapters.SurahAutoCompleteAdapter startAdapter =
                new com.repeatquran.ui.adapters.SurahAutoCompleteAdapter(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, display);
        ddStartSurah.setAdapter(startAdapter);
        ddStartSurah.setThreshold(1);
        ddStartSurah.setDropDownHeight(android.widget.ListPopupWindow.WRAP_CONTENT);
        
        com.repeatquran.ui.adapters.SurahAutoCompleteAdapter endAdapter =
                new com.repeatquran.ui.adapters.SurahAutoCompleteAdapter(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, display);
        ddEndSurah.setAdapter(endAdapter);
        ddEndSurah.setThreshold(1);
        ddEndSurah.setDropDownHeight(android.widget.ListPopupWindow.WRAP_CONTENT);

        android.content.SharedPreferences prefsRange = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE);
        int lastStart = prefsRange.getInt("last.surah.range.start", 1);
        int lastEnd = prefsRange.getInt("last.surah.range.end", 1);
        
        // Initialize start surah
        if (lastStart >= 1 && lastStart <= 114) {
            ddStartSurah.setText(com.repeatquran.util.SurahNames.display(lastStart), false);
            setupAyahDropdown(ddStartAyah, startAyahLayout, lastStart);
            lastStartSurah = lastStart; // Track initial start surah
        }
        
        // Initialize end surah - apply auto-sync logic even on initial load
        if (lastEnd >= 1 && lastEnd <= 114) {
            ddEndSurah.setText(com.repeatquran.util.SurahNames.display(lastEnd), false);
            setupAyahDropdown(ddEndAyah, endAyahLayout, lastEnd);
        } else if (lastStart >= 1 && lastStart <= 114) {
            // If no end surah saved, default to start surah
            ddEndSurah.setText(com.repeatquran.util.SurahNames.display(lastStart), false);
            setupAyahDropdown(ddEndAyah, endAyahLayout, lastStart);
        }
        
        // Restore last selected ayah values
        int lastStartAyah = prefsRange.getInt("last.ayah.range.start", 1);
        int lastEndAyah = prefsRange.getInt("last.ayah.range.end", 1);
        if (lastStartAyah > 0) ddStartAyah.setText(String.valueOf(lastStartAyah), false);
        if (lastEndAyah > 0) ddEndAyah.setText(String.valueOf(lastEndAyah), false);
        
        // Ensure all UI elements are visible initially
        ensureUIElementsVisible(root);
        
        ddStartSurah.setOnItemClickListener((parent, v, pos, id) -> {
            // Parse surah number from selected label instead of relying on filtered index
            int newStartSurah = parseSurahFromSelection(parent, pos, ddStartSurah);
            if (newStartSurah < 1 || newStartSurah > 114) return;

            setupAyahDropdown(ddStartAyah, startAyahLayout, newStartSurah);
            
            // Smart auto-sync: Copy Start → End if End is empty or End equals old Start
            int currentEndSurah = parseSurahFromText(ddEndSurah.getText() != null ? ddEndSurah.getText().toString().trim() : "");
            
            // Auto-set End Surah if: empty OR equals previous Start Surah
            if (currentEndSurah == -1 || currentEndSurah == lastStartSurah) {
                ddEndSurah.setText(com.repeatquran.util.SurahNames.display(newStartSurah), false);
                setupAyahDropdown(ddEndAyah, endAyahLayout, newStartSurah);
                Log.d("RangeTabFragment", "Auto-synced End Surah to " + newStartSurah);
            }
            
            // Update tracker
            lastStartSurah = newStartSurah;
            
            // Hide keyboard and clear focus when a Surah is selected
            ddStartSurah.dismissDropDown();
            hideKeyboard(ddStartSurah);
            ddStartSurah.clearFocus();
            if (root != null) root.requestFocus();
            // Refresh UI visibility after dropdown interaction
            ensureUIElementsVisible(root);
        });
        
        ddEndSurah.setOnItemClickListener((parent, v, pos, id) -> {
            // Parse surah number from selected label instead of relying on filtered index
            int surahNumber = parseSurahFromSelection(parent, pos, ddEndSurah);
            if (surahNumber < 1 || surahNumber > 114) return;

            setupAyahDropdown(ddEndAyah, endAyahLayout, surahNumber);
            // Hide keyboard and clear focus when a Surah is selected
            ddEndSurah.dismissDropDown();
            hideKeyboard(ddEndSurah);
            ddEndSurah.clearFocus();
            if (root != null) root.requestFocus();
            // Refresh UI visibility after dropdown interaction
            ensureUIElementsVisible(root);
        });
    }
    
    @Override 
    public void onResume() {
        super.onResume();
        // Ensure UI elements are visible when fragment becomes active
        View rootView = getView();
        if (rootView != null) {
            ensureUIElementsVisible(rootView);
        }
    }
    
    /**
     * Parse surah number from dropdown selection with robust fallback
     */
    private int parseSurahFromSelection(android.widget.AdapterView<?> parent, int pos, AutoCompleteTextView dropdown) {
        try {
            Object clicked = parent != null ? parent.getItemAtPosition(pos) : null;
            String label = clicked != null ? String.valueOf(clicked) : (dropdown.getText() != null ? dropdown.getText().toString().trim() : "");
            return parseSurahFromText(label);
        } catch (Exception ignored) {
            return -1;
        }
    }
    
    /**
     * Extract surah number from formatted text like "001 Al-Fatihah"
     */
    private int parseSurahFromText(String label) {
        if (label == null || label.length() < 3) return -1;
        
        try {
            // Try direct parse of first 3 characters
            return Integer.parseInt(label.substring(0, 3));
        } catch (Exception e) {
            // Fallback: extract leading digits if formatting differs
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < label.length() && Character.isDigit(label.charAt(i)) && sb.length() < 3; i++) {
                sb.append(label.charAt(i));
            }
            if (sb.length() > 0) {
                try {
                    return Integer.parseInt(sb.toString());
                } catch (Exception ignored) {}
            }
            return -1;
        }
    }
    
    /**
     * Ensures all UI elements are properly visible and laid out.
     * This fixes the issue where End Ayah and Play buttons may not appear.
     */
    private void ensureUIElementsVisible(View root) {
        // Force visibility of all key UI elements
        View endAyahLayout = root.findViewById(R.id.endAyahLayout);
        View endSurahLayout = root.findViewById(R.id.endSurahLayout);
        View btnPlayPause = root.findViewById(R.id.btnPlayPause);
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
        
        if (btnPlayPause != null) {
            btnPlayPause.setVisibility(View.VISIBLE);
            btnPlayPause.requestLayout();
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

    @Override
    protected void loadAndPlay() {
        View root = getView();
        if (root == null) return;
        
        TextInputLayout startSurahLayout = root.findViewById(R.id.startSurahLayout);
        TextInputLayout endSurahLayout = root.findViewById(R.id.endSurahLayout);
        TextInputLayout startAyahLayout = root.findViewById(R.id.startAyahLayout);
        TextInputLayout endAyahLayout = root.findViewById(R.id.endAyahLayout);
        
        clearError(startSurahLayout);
        clearError(endSurahLayout);
        clearError(startAyahLayout);
        clearError(endAyahLayout);
        
        // Validate reciter selection
        if (!validateReciterSelection()) return;
        
        String s1 = ddStartSurah.getText() != null ? ddStartSurah.getText().toString().trim() : "";
        String s2 = ddEndSurah.getText() != null ? ddEndSurah.getText().toString().trim() : "";
        if (s1.length() < 3) { showError(startSurahLayout, "Select start"); return; }
        if (s2.length() < 3) { showError(endSurahLayout, "Select end"); return; }
        
        int ss, es;
        try { ss = Integer.parseInt(s1.substring(0, 3)); } catch (Exception e) { showError(startSurahLayout, "Select start"); return; }
        try { es = Integer.parseInt(s2.substring(0, 3)); } catch (Exception e) { showError(endSurahLayout, "Select end"); return; }
        if (ss < 1 || ss > 114) { showError(startSurahLayout, "1..114"); return; }
        if (es < 1 || es > 114) { showError(endSurahLayout, "1..114"); return; }
        
        int sa = parseIntSafe(ddStartAyah);
        int ea = parseIntSafe(ddEndAyah);
        int maxStartAyah = getAyahCount(ss);
        int maxEndAyah = getAyahCount(es);
        
        if (sa < 1 || sa > maxStartAyah) { showError(startAyahLayout, "Ayah 1.." + maxStartAyah); return; }
        if (ea < 1 || ea > maxEndAyah) { showError(endAyahLayout, "Ayah 1.." + maxEndAyah); return; }
        if (!isStartBeforeOrEqual(ss, sa, es, ea)) { 
            showError(endSurahLayout, "End before start"); 
            showError(endAyahLayout, "End before start"); 
            return; 
        }

        // Save state
        requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).edit()
                .putInt("last.surah.range.start", ss)
                .putInt("last.surah.range.end", es)
                .putInt("last.ayah.range.start", sa)
                .putInt("last.ayah.range.end", ea)
                .apply();

        int repeat = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("repeat.count", 1);
        boolean half = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getBoolean("ui.half.split", false);

        // Disable button immediately and show loading state
        playPauseButton.setEnabled(false);
        android.widget.Toast.makeText(requireContext(), "Loading range…", android.widget.Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(requireContext(), PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_LOAD_RANGE);
        intent.putExtra("ss", ss);
        intent.putExtra("sa", sa);
        intent.putExtra("es", es);
        intent.putExtra("ea", ea);
        intent.putExtra("repeat", repeat);
        intent.putExtra("halfSplit", half);
        
        sendService(null, intent);
        
        // Set button to loading state with cooldown
        setButtonLoadingState(1200);
    }

    @Override
    protected boolean isContentForThisFragment() {
        try {
            // Check the resume state from SharedPreferences
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE);
            String sourceType = prefs.getString("resume.sourceType", "");
            
            // This fragment handles "range" content
            boolean isRangeType = "range".equals(sourceType);
            Log.d("RangeTabFragment", "Content validation: sourceType=" + sourceType + ", isRange=" + isRangeType);
            
            return isRangeType;
        } catch (Exception e) {
            Log.e("RangeTabFragment", "Error checking content ownership", e);
            return false;
        }
    }

    @Override
    protected void onSaveFragmentState(@NonNull Bundle outState) {
        // Save dropdown states
        if (ddStartSurah != null && ddStartSurah.getText() != null) {
            outState.putString("startSurah", ddStartSurah.getText().toString());
        }
        if (ddEndSurah != null && ddEndSurah.getText() != null) {
            outState.putString("endSurah", ddEndSurah.getText().toString());
        }
        if (ddStartAyah != null && ddStartAyah.getText() != null) {
            outState.putString("startAyah", ddStartAyah.getText().toString());
        }
        if (ddEndAyah != null && ddEndAyah.getText() != null) {
            outState.putString("endAyah", ddEndAyah.getText().toString());
        }
        outState.putInt("lastStartSurah", lastStartSurah);
    }

    @Override
    protected void onRestoreFragmentState(@NonNull Bundle savedInstanceState) {
        // Restore dropdown states
        String startSurah = savedInstanceState.getString("startSurah");
        String endSurah = savedInstanceState.getString("endSurah");
        String startAyah = savedInstanceState.getString("startAyah");
        String endAyah = savedInstanceState.getString("endAyah");
        
        if (startSurah != null && ddStartSurah != null) {
            ddStartSurah.setText(startSurah, false);
        }
        if (endSurah != null && ddEndSurah != null) {
            ddEndSurah.setText(endSurah, false);
        }
        if (startAyah != null && ddStartAyah != null) {
            ddStartAyah.setText(startAyah, false);
        }
        if (endAyah != null && ddEndAyah != null) {
            ddEndAyah.setText(endAyah, false);
        }
        
        lastStartSurah = savedInstanceState.getInt("lastStartSurah", -1);
    }
    
    private void setupAyahDropdown(AutoCompleteTextView ddAyah, TextInputLayout ayahLayout, int surahNumber) {
        int maxAyah = getAyahCount(surahNumber);
        
        // Capture current value to preserve if still valid
        String currentText = ddAyah.getText() != null ? ddAyah.getText().toString().trim() : "";
        int currentVal = -1;
        try { currentVal = Integer.parseInt(currentText); } catch (Exception ignored) {}
        
        // Create list of ayah numbers
        java.util.List<String> ayahNumbers = new java.util.ArrayList<>();
        for (int i = 1; i <= maxAyah; i++) {
            ayahNumbers.add(String.valueOf(i));
        }
        
        // Create adapter - use dropdown layout for rotation resistance
        ArrayAdapter<String> ayahAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, ayahNumbers);
        ddAyah.setAdapter(ayahAdapter);
        ddAyah.setThreshold(Integer.MAX_VALUE); // Disable text filtering to show all items
        ddAyah.setDropDownHeight(android.widget.ListPopupWindow.WRAP_CONTENT);
        
        // When ayah is selected from dropdown, dismiss keyboard
        ddAyah.setOnItemClickListener((p, v, pos, id) -> {
            // Dismiss dropdown and keyboard reliably
            ddAyah.dismissDropDown();
            hideKeyboard(ddAyah);
            ddAyah.clearFocus();
            View rootView = getView();
            if (rootView != null) rootView.requestFocus();
        });
        
        // Decide what value to show after surah change
        String newText;
        if (currentVal >= 1 && currentVal <= maxAyah) {
            newText = String.valueOf(currentVal);
        } else if (currentVal > maxAyah) {
            newText = String.valueOf(maxAyah); // clamp down to max
        } else {
            newText = "1"; // default
        }
        ddAyah.setText(newText, false);
        ayahLayout.setHelperText("Max ayah: " + maxAyah);
        // Clear any prior error if value is now valid
        try {
            int nv = Integer.parseInt(newText);
            if (nv >= 1 && nv <= maxAyah) clearError(ayahLayout);
        } catch (Exception ignored) {}
    }
    
    private boolean isStartBeforeOrEqual(int ss, int sa, int es, int ea) {
        if (ss < es) return true; 
        if (ss > es) return false; 
        return sa <= ea;
    }
    
    private int getAyahCount(int surah) { 
        return com.repeatquran.util.AyahCounts.getCount(surah); 
    }
}
