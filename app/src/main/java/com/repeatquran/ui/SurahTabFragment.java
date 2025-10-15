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
        });
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
        
        sendService(null, intent);
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
            
            // This fragment handles "surah" content
            boolean isSurahType = "surah".equals(sourceType);
            Log.d(getFragmentTag(), "Content validation: sourceType=" + sourceType + ", isSurah=" + isSurahType);
            
            return isSurahType;
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
}
