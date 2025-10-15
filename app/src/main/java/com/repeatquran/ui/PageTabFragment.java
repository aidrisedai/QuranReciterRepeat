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

public class PageTabFragment extends BaseTabFragment {
    private AutoCompleteTextView ddPage;
    
    @Override
    protected String getFragmentTag() {
        return "PageTabFragment";
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_page_tab, container, false);
        setupUi(v);
        setupCommonButtons(v);
        return v;
    }

    private void setupUi(View root) {
        TextInputLayout pageLayout = root.findViewById(R.id.pageInputLayout);
        ddPage = root.findViewById(R.id.pageDropdown);
        
        // Set up page dropdown with common pages (1-604)
        setupPageDropdown(ddPage, pageLayout);
        
        // Restore last selected page
        int last = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("last.page", 1);
        ddPage.setText(String.valueOf(last), false);
    }
    
    private void setupPageDropdown(AutoCompleteTextView ddPage, TextInputLayout pageLayout) {
        // Create list of common page numbers (we'll make it searchable)
        java.util.List<String> pageNumbers = new java.util.ArrayList<>();
        // Add common page ranges for easy selection
        for (int i = 1; i <= 604; i += 10) {
            pageNumbers.add(String.valueOf(i));
        }
        // Add last 10 pages individually for common use
        for (int i = 595; i <= 604; i++) {
            if (!pageNumbers.contains(String.valueOf(i))) {
                pageNumbers.add(String.valueOf(i));
            }
        }
        
        // Create adapter with filtering support - use dropdown layout for rotation resistance
        ArrayAdapter<String> pageAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, pageNumbers) {
            @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        java.util.List<String> allPages = new java.util.ArrayList<>();
                        for (int i = 1; i <= 604; i++) {
                            allPages.add(String.valueOf(i));
                        }
                        
                        if (constraint == null || constraint.length() == 0) {
                            results.values = pageNumbers; // Show common pages by default
                            results.count = pageNumbers.size();
                        } else {
                            java.util.List<String> filtered = new java.util.ArrayList<>();
                            String filterString = constraint.toString().toLowerCase();
                            for (String page : allPages) {
                                if (page.startsWith(filterString)) {
                                    filtered.add(page);
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
                            addAll((java.util.List<String>) results.values);
                            notifyDataSetChanged();
                        }
                    }
                };
            }
        };
        
        ddPage.setAdapter(pageAdapter);
        ddPage.setThreshold(Integer.MAX_VALUE); // Disable text filtering to show all items
        ddPage.setDropDownHeight(android.widget.ListPopupWindow.WRAP_CONTENT);
        
        // When page is selected from dropdown, dismiss keyboard
        ddPage.setOnItemClickListener((p, v, pos, id) -> {
            // Dismiss dropdown and keyboard reliably
            ddPage.dismissDropDown();
            hideKeyboard(ddPage);
            ddPage.clearFocus();
            View rootView = getView();
            if (rootView != null) rootView.requestFocus();
            // Update navigation button states
            updateNavigationButtons();
        });
        
        pageLayout.setHelperText("Enter page 1–604");
    }

    @Override
    protected void loadAndPlay() {
        View root = getView();
        if (root == null) return;
        
        TextInputLayout pageLayout = root.findViewById(R.id.pageInputLayout);
        clearError(pageLayout);
        
        int page = parseIntSafe(ddPage);
        if (page < 1 || page > 604) { 
            showError(pageLayout, "Enter 1–604"); 
            return; 
        }
        
        // Validate reciter selection
        if (!validateReciterSelection()) return;
        
        requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
            .edit()
            .putInt("last.page", page)
            .apply();
            
        int repeat = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
            .getInt("repeat.count", 1);
        boolean half = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
            .getBoolean("ui.half.split", false);
        
        Intent intent = new Intent(requireContext(), PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_LOAD_PAGE);
        intent.putExtra("page", page);
        intent.putExtra("repeat", repeat);
        intent.putExtra("halfSplit", half);
        
        sendService(PlaybackService.ACTION_LOAD_PAGE, intent);
        
        android.widget.Toast.makeText(requireContext(), 
            "Loading page " + page + "…", 
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
            
            // This fragment handles "page" content
            boolean isPageType = "page".equals(sourceType);
            Log.d(getFragmentTag(), "Content validation: sourceType=" + sourceType + ", isPage=" + isPageType);
            
            return isPageType;
        } catch (Exception e) {
            Log.e(getFragmentTag(), "Error checking content ownership", e);
            return false;
        }
    }

    @Override
    protected void onSaveFragmentState(@NonNull Bundle outState) {
        // Save dropdown state
        if (ddPage != null && ddPage.getText() != null) {
            outState.putString("page", ddPage.getText().toString());
        }
    }

    @Override
    protected void onRestoreFragmentState(@NonNull Bundle savedInstanceState) {
        // Restore dropdown state
        String page = savedInstanceState.getString("page");
        if (page != null && ddPage != null) {
            ddPage.setText(page, false);
        }
    }
    
    // ==================== NAVIGATION ====================
    
    @Override
    protected boolean navigatePrevious() {
        int currentPage = getCurrentPage();
        if (currentPage > 1) {
            setPage(currentPage - 1);
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean navigateNext() {
        int currentPage = getCurrentPage();
        if (currentPage < 604) {
            setPage(currentPage + 1);
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean canNavigatePrevious() {
        int currentPage = getCurrentPage();
        return currentPage > 1;
    }
    
    @Override
    protected boolean canNavigateNext() {
        int currentPage = getCurrentPage();
        return currentPage < 604;
    }
    
    /**
     * Get current page number from dropdown.
     * @return current page number, or 1 if invalid
     */
    private int getCurrentPage() {
        if (ddPage == null) return 1;
        int page = parseIntSafe(ddPage);
        return (page >= 1 && page <= 604) ? page : 1;
    }
    
    /**
     * Set page number in dropdown.
     * @param page page number to set (1-604)
     */
    private void setPage(int page) {
        if (ddPage != null && page >= 1 && page <= 604) {
            ddPage.setText(String.valueOf(page), false);
            // Save to preferences
            requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
                .edit()
                .putInt("last.page", page)
                .apply();
            Log.d(getFragmentTag(), "Page set to: " + page);
        }
    }
}
