package com.repeatquran.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import com.repeatquran.R;
import com.repeatquran.playback.PlaybackService;

/**
 * Fragment for single verse (ayah) playback.
 * 
 * Features:
 * - Search-as-you-type surah selection
 * - Real-time ayah validation
 * - Smart ayah dropdown that updates when surah changes
 */
public class VerseTabFragment extends BaseTabFragment {
    
    // Fragment-specific fields for state persistence
    private AutoCompleteTextView ddSurah;
    private AutoCompleteTextView ddAyah;
    
    @Override
    protected String getFragmentTag() {
        return "VerseTabFragment";
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(getFragmentTag(), "onCreateView called");
        View v = inflater.inflate(R.layout.fragment_verse_tab, container, false);
        setupUi(v);
        Log.d(getFragmentTag(), "onCreateView completed");
        return v;
    }
    
    private void setupUi(View root) {
        Log.d(getFragmentTag(), "setupUi called");
        
        // Store references for state persistence
        ddSurah = root.findViewById(R.id.surahDropdown);
        ddAyah = root.findViewById(R.id.ayahDropdown);
        TextInputLayout surahLayout = root.findViewById(R.id.surahInputLayout);
        TextInputLayout ayahLayout = root.findViewById(R.id.ayahInputLayout);

        String[] display = com.repeatquran.util.SurahNames.displayList();
        // Search-as-you-type adapter for Surah selection
        com.repeatquran.ui.adapters.SurahAutoCompleteAdapter surahAdapter =
                new com.repeatquran.ui.adapters.SurahAutoCompleteAdapter(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, display);
        ddSurah.setAdapter(surahAdapter);
        ddSurah.setThreshold(1); // Enable filtering after 1 character
        ddSurah.setDropDownHeight(android.widget.ListPopupWindow.WRAP_CONTENT);
        
        // Prefill last surah if available
        int lastSurah = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("last.surah.single", 1);
        if (lastSurah >= 1 && lastSurah <= 114) {
            ddSurah.setText(com.repeatquran.util.SurahNames.display(lastSurah), false);
            setupAyahDropdown(ddAyah, ayahLayout, lastSurah);
        }

        // When surah changes, update ayah dropdown and hide keyboard
        ddSurah.setOnItemClickListener((p, v, pos, id) -> {
            int surahNumber = pos + 1;
            setupAyahDropdown(ddAyah, ayahLayout, surahNumber);
            // Dismiss dropdown and keyboard
            ddSurah.dismissDropDown();
            hideKeyboard(ddSurah);
            ddSurah.clearFocus();
            if (root != null) root.requestFocus();
            // Update navigation button states
            updateNavigationButtons();
        });

        // Setup common buttons (Play/Pause, Stop, Previous, Next) - inherited from base
        setupCommonButtons(root);
        
        // Update navigation buttons after setup
        updateNavigationButtons();
        
        // Test-only helper to seed a reciter
        setupTestEnvironment();
    }

    @Override
    protected void loadAndPlay() {
        View root = getView();
        if (root == null) return;
        
        TextInputLayout surahLayout = root.findViewById(R.id.surahInputLayout);
        TextInputLayout ayahLayout = root.findViewById(R.id.ayahInputLayout);
        
        // Validate reciter selection
        if (!validateReciterSelection()) return;
        
        // Clear errors
        clearError(surahLayout);
        clearError(ayahLayout);
        
        // Validate surah
        String txt = ddSurah.getText() != null ? ddSurah.getText().toString().trim() : "";
        if (txt.length() < 3) {
            showError(surahLayout, "Select surah");
            return;
        }
        
        int surah;
        try {
            surah = Integer.parseInt(txt.substring(0, 3));
        } catch (Exception e) {
            showError(surahLayout, "Select surah");
            return;
        }
        
        if (surah < 1 || surah > 114) {
            showError(surahLayout, "1..114");
            return;
        }
        
        // Validate ayah
        int ayah = parseIntSafe(ddAyah);
        if (ayah < 1 || ayah > getAyahCount(surah)) {
            showError(ayahLayout, "Ayah 1.." + getAyahCount(surah));
            return;
        }
        
        // Persist last selected
        requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
                .edit().putInt("last.surah.single", surah).apply();
        
        // Get repeat count from prefs
        int repeat = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("repeat.count", 1);
        
        // Create intent
        Intent intent = new Intent(requireContext(), PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_LOAD_SINGLE);
        intent.putExtra("sura", surah);
        intent.putExtra("ayah", ayah);
        intent.putExtra("repeat", repeat);
        
        // Send to service
        sendService(PlaybackService.ACTION_LOAD_SINGLE, intent);
        
        // Show feedback
        android.widget.Toast.makeText(requireContext(),
                "Loading Surah " + String.format("%03d", surah) + " — " + com.repeatquran.util.SurahNames.name(surah) +
                        ", Ayah " + ayah + " (repeat=" + (repeat == -1 ? "∞" : repeat) + ")",
                android.widget.Toast.LENGTH_SHORT).show();
        
        // Set loading state (1.2s cooldown)
        setButtonLoadingState(1200);
    }
    
    @Override
    protected boolean isContentForThisFragment() {
        try {
            android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE);
            String sourceType = prefs.getString("resume.sourceType", "");
            int resumeSurah = prefs.getInt("resume.startSurah", -1);
            int resumeAyah = prefs.getInt("resume.startAyah", -1);
            int currentSurah = getCurrentSurah();
            int currentAyah = getCurrentAyah();
            
            // This fragment handles "single" ayah content AND verse must match current selection
            boolean isSingleType = "single".equals(sourceType);
            boolean verseMatches = (resumeSurah == currentSurah && resumeAyah == currentAyah);
            boolean isOwner = isSingleType && verseMatches;
            
            Log.d(getFragmentTag(), "Content validation: sourceType=" + sourceType + 
                  ", resumeVerse=" + resumeSurah + ":" + resumeAyah + 
                  ", currentVerse=" + currentSurah + ":" + currentAyah + 
                  ", isOwner=" + isOwner);
            
            return isOwner;
        } catch (Exception e) {
            Log.e(getFragmentTag(), "Error checking content ownership", e);
            return false;
        }
    }
    
    @Override
    protected void onSaveFragmentState(@NonNull Bundle outState) {
        if (ddSurah != null && ddSurah.getText() != null) {
            outState.putString("surah_text", ddSurah.getText().toString());
        }
        if (ddAyah != null && ddAyah.getText() != null) {
            outState.putString("ayah_text", ddAyah.getText().toString());
        }
    }
    
    @Override
    protected void onRestoreFragmentState(@NonNull Bundle savedInstanceState) {
        String surahText = savedInstanceState.getString("surah_text");
        String ayahText = savedInstanceState.getString("ayah_text");
        
        if (surahText != null && ddSurah != null) {
            ddSurah.setText(surahText, false);
            
            // Parse surah number and setup ayah dropdown
            try {
                if (surahText.length() >= 3) {
                    int surah = Integer.parseInt(surahText.substring(0, 3));
                    if (surah >= 1 && surah <= 114 && ddAyah != null) {
                        TextInputLayout ayahLayout = getView().findViewById(R.id.ayahInputLayout);
                        setupAyahDropdown(ddAyah, ayahLayout, surah);
                    }
                }
            } catch (Exception ignored) {}
        }
        
        if (ayahText != null && ddAyah != null) {
            ddAyah.setText(ayahText, false);
        }
    }
    
    // ==================== VERSE-SPECIFIC HELPERS ====================
    
    private void setupAyahDropdown(AutoCompleteTextView ddAyah, TextInputLayout ayahLayout, int surahNumber) {
        int maxAyah = getAyahCount(surahNumber);
        
        // Create list of ayah numbers
        List<String> ayahNumbers = new ArrayList<>();
        for (int i = 1; i <= maxAyah; i++) {
            ayahNumbers.add(String.valueOf(i));
        }
        
        // Create adapter with filtering support
        ArrayAdapter<String> ayahAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, ayahNumbers) {
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
        
        // When ayah is selected, dismiss keyboard
        ddAyah.setOnItemClickListener((p, v, pos, id) -> {
            ddAyah.dismissDropDown();
            hideKeyboard(ddAyah);
            ddAyah.clearFocus();
            View rootView = getView();
            if (rootView != null) rootView.requestFocus();
            // Update navigation button states
            updateNavigationButtons();
        });
        
        // Set default to ayah 1
        ddAyah.setText("1", false);
        ayahLayout.setHelperText("Max ayah: " + maxAyah);
        clearError(ayahLayout);
        
        // Add real-time validation with red border
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
                        ayahLayout.setHelperText("Max ayah: " + maxAyah);
                        ayahLayout.setError(" "); // Red border without message
                    } else {
                        clearError(ayahLayout);
                        ayahLayout.setHelperText("Max ayah: " + maxAyah);
                    }
                } catch (NumberFormatException e) {
                    ayahLayout.setHelperText("Max ayah: " + maxAyah);
                    ayahLayout.setError(" "); // Red border without message
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
    
    // ==================== NAVIGATION ====================
    
    @Override
    protected boolean navigatePrevious() {
        int currentSurah = getCurrentSurah();
        int currentAyah = getCurrentAyah();
        
        if (currentSurah <= 0 || currentAyah <= 0) return false;
        
        // Move to previous ayah
        if (currentAyah > 1) {
            setVerse(currentSurah, currentAyah - 1);
            return true;
        }
        
        // At first ayah - move to previous surah's last ayah
        if (currentSurah > 1) {
            int prevSurah = currentSurah - 1;
            int lastAyah = getAyahCount(prevSurah);
            setVerse(prevSurah, lastAyah);
            return true;
        }
        
        return false; // At first ayah of first surah
    }
    
    @Override
    protected boolean navigateNext() {
        int currentSurah = getCurrentSurah();
        int currentAyah = getCurrentAyah();
        
        if (currentSurah <= 0 || currentAyah <= 0) return false;
        
        int maxAyah = getAyahCount(currentSurah);
        
        // Move to next ayah
        if (currentAyah < maxAyah) {
            setVerse(currentSurah, currentAyah + 1);
            return true;
        }
        
        // At last ayah - move to next surah's first ayah
        if (currentSurah < 114) {
            setVerse(currentSurah + 1, 1);
            return true;
        }
        
        return false; // At last ayah of last surah
    }
    
    @Override
    protected boolean canNavigatePrevious() {
        int currentSurah = getCurrentSurah();
        int currentAyah = getCurrentAyah();
        
        // Can't go previous if at first ayah of first surah
        return !(currentSurah == 1 && currentAyah == 1);
    }
    
    @Override
    protected boolean canNavigateNext() {
        int currentSurah = getCurrentSurah();
        int currentAyah = getCurrentAyah();
        
        if (currentSurah <= 0) return false;
        
        int maxAyah = getAyahCount(currentSurah);
        
        // Can't go next if at last ayah of last surah
        return !(currentSurah == 114 && currentAyah == maxAyah);
    }
    
    /**
     * Get current surah number from dropdown.
     * @return current surah number, or -1 if invalid
     */
    private int getCurrentSurah() {
        if (ddSurah == null || ddSurah.getText() == null) return -1;
        
        String text = ddSurah.getText().toString().trim();
        if (text.length() < 3) return -1;
        
        try {
            int surah = Integer.parseInt(text.substring(0, 3));
            return (surah >= 1 && surah <= 114) ? surah : -1;
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Get current ayah number from dropdown.
     * @return current ayah number, or -1 if invalid
     */
    private int getCurrentAyah() {
        if (ddAyah == null) return -1;
        int ayah = parseIntSafe(ddAyah);
        int currentSurah = getCurrentSurah();
        
        if (currentSurah <= 0) return -1;
        
        int maxAyah = getAyahCount(currentSurah);
        return (ayah >= 1 && ayah <= maxAyah) ? ayah : -1;
    }
    
    /**
     * Set verse (surah and ayah) in dropdowns.
     * @param surah surah number (1-114)
     * @param ayah ayah number (1-max for that surah)
     */
    private void setVerse(int surah, int ayah) {
        if (surah < 1 || surah > 114) return;
        
        int maxAyah = getAyahCount(surah);
        if (ayah < 1 || ayah > maxAyah) return;
        
        // Update surah dropdown
        if (ddSurah != null) {
            ddSurah.setText(com.repeatquran.util.SurahNames.display(surah), false);
        }
        
        // Update ayah dropdown (need to setup dropdown for new surah first)
        if (ddAyah != null && getView() != null) {
            TextInputLayout ayahLayout = getView().findViewById(R.id.ayahInputLayout);
            setupAyahDropdown(ddAyah, ayahLayout, surah);
            ddAyah.setText(String.valueOf(ayah), false);
        }
        
        // Save to preferences
        requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
            .edit()
            .putInt("last.surah.single", surah)
            .apply();
        
        Log.d(getFragmentTag(), "Verse set to: Surah " + surah + ", Ayah " + ayah);
    }
    
    private void setupTestEnvironment() {
        try {
            // In debug builds only, seed one reciter to ease local testing
            if (android.os.Build.FINGERPRINT != null && android.os.Build.FINGERPRINT.contains("robolectric")) {
                String savedOrder = requireContext()
                    .getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
                    .getString("reciters.order", "");
                if (savedOrder == null || savedOrder.trim().isEmpty()) {
                    requireContext()
                        .getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
                        .edit()
                        .putString("reciters.order", "Abdurrahmaan_As-Sudais_64kbps")
                        .apply();
                    Log.d(getFragmentTag(), "Setup test reciter (tests only)");
                }
            }
        } catch (Exception ignored) {}
    }
}
