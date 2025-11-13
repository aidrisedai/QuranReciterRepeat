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

public class SurahTabFragment extends BaseTabFragment {
    private AutoCompleteTextView ddSurah;
    
    @Override
    protected String getFragmentTag() {
        return "SurahTabFragment";
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_surah_tab, container, false);
        setupUi(v);
        setupCommonButtons(v);
        return v;
    }

    private void setupUi(View root) {
        Log.d(getFragmentTag(), "setupUi called");
        ddSurah = root.findViewById(R.id.surahDropdown);

        String[] display = com.repeatquran.util.SurahNames.displayList();
        // Use dropdown layout for maximum compatibility and rotation resistance
        // NOTE: No filtering threshold (Integer.MAX_VALUE) - this is intentional UX for Surah tab
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_dropdown_item_1line, display);
        ddSurah.setAdapter(adapter);
        ddSurah.setThreshold(Integer.MAX_VALUE); // Disable text filtering to show all items
        ddSurah.setDropDownHeight(android.widget.ListPopupWindow.WRAP_CONTENT);

        int last = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("last.surah", 1);
        if (last >= 1 && last <= 114) {
            ddSurah.setText(com.repeatquran.util.SurahNames.display(last), false);
        }

        // Close keyboard on Surah selection
        ddSurah.setOnItemClickListener((p, v, pos, id) -> {
            ddSurah.dismissDropDown();
            hideKeyboard(ddSurah);
            ddSurah.clearFocus();
            View rv = getView();
            if (rv != null) rv.requestFocus();
            // Update navigation button states
            updateNavigationButtons();
        });
        
        // Update navigation buttons after setup
        updateNavigationButtons();
    }

    @Override
    protected void loadAndPlay() {
        View root = getView();
        if (root == null) return;
        
        TextInputLayout layout = root.findViewById(R.id.surahSelectLayout);
        clearError(layout);
        
        String txt = ddSurah.getText() != null ? ddSurah.getText().toString().trim() : "";
        if (txt.length() < 3) { 
            showError(layout, "Select a surah"); 
            return; 
        }
        
        int surah;
        try { 
            surah = Integer.parseInt(txt.substring(0, 3)); 
        } catch (Exception e) { 
            showError(layout, "Select a surah"); 
            return; 
        }
        
        if (surah < 1 || surah > 114) { 
            showError(layout, "Invalid surah"); 
            return; 
        }
        
        requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
            .edit()
            .putInt("last.surah", surah)
            .apply();
            
        int repeat = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
            .getInt("repeat.count", 1);
        boolean half = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
            .getBoolean("ui.half.split", false);
        
        Intent intent = new Intent(requireContext(), PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_LOAD_SURAH);
        intent.putExtra("surah", surah);
        intent.putExtra("repeat", repeat);
        intent.putExtra("halfSplit", half);
        
        sendService(PlaybackService.ACTION_LOAD_SURAH, intent);
        android.widget.Toast.makeText(requireContext(), 
            "Loading surah " + String.format("%03d", surah) + "…", 
            android.widget.Toast.LENGTH_SHORT).show();
        
        // Set button to loading state with cooldown
        setButtonLoadingState(1200);
    }

    @Override
    protected boolean isContentForThisFragment() {
        try {
            // Check the resume state from SharedPreferences
            android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE);
            String sourceType = prefs.getString("resume.sourceType", "");
            int resumeSurah = prefs.getInt("resume.startSurah", -1);
            int currentSurah = getCurrentSurah();
            
            // This fragment handles "surah" content AND the surah number must match current selection
            boolean isSurahType = "surah".equals(sourceType);
            boolean surahMatches = (resumeSurah == currentSurah);
            boolean isOwner = isSurahType && surahMatches;
            
            Log.d(getFragmentTag(), "Content validation: sourceType=" + sourceType + 
                  ", resumeSurah=" + resumeSurah + ", currentSurah=" + currentSurah + 
                  ", isOwner=" + isOwner);
            
            return isOwner;
        } catch (Exception e) {
            Log.e(getFragmentTag(), "Error checking content ownership", e);
            return false;
        }
    }

    @Override
    protected void onSaveFragmentState(@NonNull Bundle outState) {
        // Save dropdown state
        if (ddSurah != null && ddSurah.getText() != null) {
            outState.putString("surah", ddSurah.getText().toString());
        }
    }

    @Override
    protected void onRestoreFragmentState(@NonNull Bundle savedInstanceState) {
        // Restore dropdown state
        String surah = savedInstanceState.getString("surah");
        if (surah != null && ddSurah != null) {
            ddSurah.setText(surah, false);
        }
    }
    
    // ==================== NAVIGATION ====================
    
    @Override
    protected boolean navigatePrevious() {
        int currentSurah = getCurrentSurah();
        if (currentSurah > 1) {
            setSurah(currentSurah - 1);
            return true;
        }
        return false; // At first surah
    }
    
    @Override
    protected boolean navigateNext() {
        int currentSurah = getCurrentSurah();
        if (currentSurah < 114) {
            setSurah(currentSurah + 1);
            return true;
        }
        return false; // At last surah
    }
    
    @Override
    protected boolean canNavigatePrevious() {
        int currentSurah = getCurrentSurah();
        return currentSurah > 1;
    }
    
    @Override
    protected boolean canNavigateNext() {
        int currentSurah = getCurrentSurah();
        return currentSurah < 114;
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
     * Set surah in dropdown.
     * @param surah surah number (1-114)
     */
    private void setSurah(int surah) {
        if (surah < 1 || surah > 114) return;
        
        if (ddSurah != null) {
            ddSurah.setText(com.repeatquran.util.SurahNames.display(surah), false);
        }
        
        // Save to preferences
        requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
            .edit()
            .putInt("last.surah", surah)
            .apply();
        
        Log.d(getFragmentTag(), "Surah set to: " + surah);
    }
}
