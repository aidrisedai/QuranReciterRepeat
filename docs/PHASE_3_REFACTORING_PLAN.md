# Phase 3 Refactoring Implementation Plan

**Date:** October 15, 2025  
**Goal:** Extract BaseTabFragment to consolidate ~440 lines of duplicate code  
**Approach:** Refactor First → Fix Second (Smart Order)  
**Status:** Implementation Plan - Ready for Execution

---

## Table of Contents

1. [Overview](#overview)
2. [Phase 1: Create BaseTabFragment](#phase-1-create-basetabfragment)
3. [Phase 2: Migrate VerseTabFragment](#phase-2-migrate-versetabfragment)
4. [Phase 3: Migrate RangeTabFragment](#phase-3-migrate-rangetabfragment)
5. [Phase 4: Migrate SurahTabFragment](#phase-4-migrate-surahtabfragment)
6. [Phase 5: Migrate PageTabFragment](#phase-5-migrate-pagetabfragment)
7. [Phase 6: Add State Persistence](#phase-6-add-state-persistence)
8. [Testing Strategy](#testing-strategy)
9. [Rollback Plan](#rollback-plan)

---

## Overview

### Why This Order?

**Refactor First (Step 2) → Fix Second (Step 1)** because:
- Write state persistence code **once** instead of 4 times
- Reduce total codebase by ~440 lines
- Make future maintenance 4x easier
- Avoid multiplying bugs

### What We're Building

```
Before:
VerseTabFragment (506 lines)    ← 70% duplicate
RangeTabFragment (574 lines)    ← 70% duplicate  
SurahTabFragment (321 lines)    ← 75% duplicate
PageTabFragment (385 lines)     ← 72% duplicate

After:
BaseTabFragment (250 lines)     ← All common code
├── VerseTabFragment (150 lines)   ← Only verse-specific
├── RangeTabFragment (220 lines)   ← Only range-specific
├── SurahTabFragment (80 lines)    ← Only surah-specific
└── PageTabFragment (120 lines)    ← Only page-specific

Total reduction: ~440 lines removed
```

### Migration Strategy

**Incremental, one fragment at a time:**
1. Create BaseTabFragment (skeleton)
2. Migrate VerseTab → test → commit
3. Migrate RangeTab → test → commit
4. Migrate SurahTab → test → commit
5. Migrate PageTab → test → commit
6. Add state persistence to Base → all fragments inherit

**Safety:** Each migration is tested independently. If issues arise, we can roll back one fragment at a time.

---

## Phase 1: Create BaseTabFragment

### Step 1.1: Create Base Class File

**File:** `app/src/main/java/com/repeatquran/ui/BaseTabFragment.java`

**Full Implementation:**

```java
package com.repeatquran.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.repeatquran.R;
import com.repeatquran.playback.PlaybackService;
import com.repeatquran.playback.PlaybackStateManager;

/**
 * Base class for all tab fragments (Verse, Range, Surah, Page).
 * 
 * Contains common functionality:
 * - Play/Pause/Stop button state management
 * - PlaybackStateManager integration
 * - Service communication
 * - Validation helpers
 * - Lifecycle management
 * 
 * Subclasses must implement:
 * - getFragmentTag(): Unique tag for logging
 * - loadAndPlay(): Fragment-specific content loading
 * - isContentForThisFragment(): Content ownership check
 */
public abstract class BaseTabFragment extends Fragment 
    implements PlaybackStateManager.FragmentStateChangeListener {
    
    // ==================== COMMON STATE ====================
    
    protected com.google.android.exoplayer2.ExoPlayer player;
    protected MaterialButton playPauseButton;
    protected boolean isCurrentlyPlaying = false;
    protected long reenableAtMs = 0L; // Debounce cooldown
    protected boolean justStopped = false; // Prevent state override after stop
    
    // ==================== ABSTRACT METHODS ====================
    
    /**
     * Get unique tag for this fragment (for logging).
     * Example: "VerseTabFragment"
     */
    protected abstract String getFragmentTag();
    
    /**
     * Load and play content specific to this fragment.
     * Called when Play button is pressed and no content is loaded,
     * or when content belongs to a different tab.
     */
    protected abstract void loadAndPlay();
    
    /**
     * Check if currently loaded content belongs to this fragment.
     * Uses SharedPreferences "resume.sourceType" to determine ownership.
     * 
     * @return true if content belongs to this fragment, false otherwise
     */
    protected abstract boolean isContentForThisFragment();
    
    // ==================== LIFECYCLE ====================
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(getFragmentTag(), "onCreate called");
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(getFragmentTag(), "onResume - requesting immediate state update");
        // Force an immediate state update when fragment becomes visible
        PlaybackStateManager.getInstance().forceStateUpdate();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(getFragmentTag(), "onPause called");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove listener from centralized manager
        PlaybackStateManager.getInstance().removeListener(this);
        Log.d(getFragmentTag(), "onDestroy - removed from PlaybackStateManager");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up view references to prevent memory leaks
        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(null);
        }
        playPauseButton = null;
        player = null;
        Log.d(getFragmentTag(), "onDestroyView - cleaned up view references");
    }
    
    // ==================== SETUP HELPERS ====================
    
    /**
     * Setup common UI elements (Play/Pause and Stop buttons).
     * Must be called by subclass after inflating the view.
     * 
     * @param root The fragment's root view
     */
    protected void setupCommonButtons(View root) {
        playPauseButton = root.findViewById(R.id.btnPlayPause);
        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(v -> {
                Log.d(getFragmentTag(), "Play/Pause button clicked!");
                handlePlayPauseToggle();
            });
        }
        
        View stopButton = root.findViewById(R.id.btnStop);
        if (stopButton != null) {
            stopButton.setOnClickListener(v -> {
                Log.d(getFragmentTag(), "STOP clicked - resetting state");
                handleStopButton();
            });
        }
        
        // Register for centralized state updates
        PlaybackStateManager.getInstance().addListener(this);
    }
    
    // ==================== PLAY/PAUSE/STOP LOGIC ====================
    
    /**
     * Handle Play/Pause button toggle.
     * Implements the complete state machine for play/pause behavior.
     */
    private void handlePlayPauseToggle() {
        // Check if in cooldown period - ignore click during service startup
        long now = android.os.SystemClock.uptimeMillis();
        if (now < reenableAtMs) {
            Log.d(getFragmentTag(), "Button click ignored - still in cooldown");
            return;
        }
        
        getPlayerReference();
        
        // Check actual player state
        boolean actuallyPlaying = player != null && player.isPlaying();
        boolean hasContent = player != null && player.getMediaItemCount() > 0;
        
        Log.d(getFragmentTag(), "=== PLAY/PAUSE TOGGLE ===");
        Log.d(getFragmentTag(), "actuallyPlaying=" + actuallyPlaying + 
              ", hasContent=" + hasContent + ", localState=" + isCurrentlyPlaying);
        Log.d(getFragmentTag(), "justStopped=" + justStopped);
        
        // Clear justStopped flag when user interacts
        justStopped = false;
        
        if (actuallyPlaying) {
            // Currently playing - pause it
            Log.d(getFragmentTag(), "Pausing playback");
            sendService(PlaybackService.ACTION_PAUSE);
            isCurrentlyPlaying = false;
            reenableAtMs = 0L; // Allow immediate interaction after pause
            updateButtonUI(false);
        } else if (hasContent) {
            // Has content but not playing - check if it belongs to this fragment
            if (isContentForThisFragment()) {
                Log.d(getFragmentTag(), "Resuming playback");
                sendService(PlaybackService.ACTION_PLAY);
                isCurrentlyPlaying = true;
                updateButtonUI(true);
                // Add cooldown to prevent periodic updater from overriding immediately
                reenableAtMs = android.os.SystemClock.uptimeMillis() + 800;
                playPauseButton.setEnabled(false);
            } else {
                Log.d(getFragmentTag(), "Content belongs to different tab - loading new content");
                loadAndPlay();
            }
        } else {
            // No content - load new
            Log.d(getFragmentTag(), "Loading new content");
            loadAndPlay();
        }
    }
    
    /**
     * Handle Stop button press.
     * Resets all playback state and updates UI.
     */
    private void handleStopButton() {
        sendService(PlaybackService.ACTION_STOP);
        
        // Reset local state when stopping
        isCurrentlyPlaying = false;
        reenableAtMs = 0L;
        player = null; // Clear player reference
        justStopped = true; // Prevent periodic updater from overriding
        
        Log.d(getFragmentTag(), "State reset: isCurrentlyPlaying=false, " +
              "reenableAtMs=0, player=null, justStopped=true");
        
        // Update button UI immediately
        if (playPauseButton != null) {
            playPauseButton.setText("Play");
            playPauseButton.setIcon(ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_play_arrow));
            playPauseButton.setEnabled(true);
        }
        
        android.widget.Toast.makeText(requireContext(), 
            "Stopped", android.widget.Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Update Play/Pause button UI based on playing state.
     * 
     * @param isPlaying true if playing, false if paused/stopped
     */
    protected void updateButtonUI(boolean isPlaying) {
        if (playPauseButton == null) return;
        
        if (isPlaying) {
            playPauseButton.setText("Pause");
            playPauseButton.setIcon(ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_pause));
        } else {
            playPauseButton.setText("Play");
            playPauseButton.setIcon(ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_play_arrow));
        }
    }
    
    /**
     * Set button to loading state with cooldown.
     * Call this after initiating content load.
     * 
     * @param cooldownMs Cooldown duration in milliseconds
     */
    protected void setButtonLoadingState(long cooldownMs) {
        if (playPauseButton == null) return;
        
        isCurrentlyPlaying = true;
        updateButtonUI(true);
        
        // Debounce: disable button briefly to prevent double-taps
        reenableAtMs = android.os.SystemClock.uptimeMillis() + cooldownMs;
        playPauseButton.setEnabled(false);
    }
    
    // ==================== STATE CHANGE LISTENER ====================
    
    /**
     * Centralized state change listener from PlaybackStateManager.
     * Updates UI based on actual player state.
     */
    @Override
    public void onPlaybackStateChanged(boolean hasQueue, boolean isPlaying, 
                                      com.google.android.exoplayer2.ExoPlayer player) {
        // Update player reference from centralized manager
        this.player = player;
        
        if (playPauseButton == null) return;
        
        // If we just stopped, don't let centralized updater override our state
        if (justStopped) {
            Log.d(getFragmentTag(), "onPlaybackStateChanged: just stopped, " +
                  "skipping update to preserve stop state");
            // Clear the flag after a short delay
            playPauseButton.postDelayed(() -> {
                justStopped = false;
                Log.d(getFragmentTag(), "justStopped flag cleared");
            }, 1000);
            return;
        }
        
        // Respect cooldown window - don't update UI during debounce period
        long now = android.os.SystemClock.uptimeMillis();
        boolean inCooldown = now < reenableAtMs;
        
        if (inCooldown) {
            // During cooldown, keep button disabled and don't change its state
            playPauseButton.setEnabled(false);
            Log.d(getFragmentTag(), "onPlaybackStateChanged: in cooldown, skipping update");
            return;
        }
        
        // Only sync state when not in cooldown
        isCurrentlyPlaying = isPlaying;
        
        Log.d(getFragmentTag(), "onPlaybackStateChanged: isPlaying=" + 
              isPlaying + ", hasQueue=" + hasQueue);
        
        // Update button UI based on centralized state
        playPauseButton.setEnabled(true);
        updateButtonUI(isPlaying);
    }
    
    // ==================== SERVICE COMMUNICATION ====================
    
    /**
     * Send action to PlaybackService.
     * Automatically handles foreground service requirements for API 26+.
     * 
     * @param action The service action (e.g., ACTION_PLAY, ACTION_PAUSE)
     */
    protected void sendService(String action) {
        sendService(action, null);
    }
    
    /**
     * Send action to PlaybackService with custom intent.
     * Preserves intent extras if provided.
     * 
     * @param action The service action
     * @param baseIntent Custom intent with extras (can be null)
     */
    protected void sendService(String action, Intent baseIntent) {
        Intent intent;
        if (baseIntent != null) {
            baseIntent.setAction(action);
            intent = baseIntent;
        } else {
            intent = new Intent(requireContext(), PlaybackService.class);
            intent.setAction(action);
        }
        
        boolean needsForeground =
                PlaybackService.ACTION_PLAY.equals(action) ||
                PlaybackService.ACTION_LOAD_SINGLE.equals(action) ||
                PlaybackService.ACTION_LOAD_RANGE.equals(action) ||
                PlaybackService.ACTION_LOAD_PAGE.equals(action) ||
                PlaybackService.ACTION_LOAD_SURAH.equals(action) ||
                PlaybackService.ACTION_RESUME.equals(action) ||
                PlaybackService.ACTION_SET_SPEED.equals(action);
        
        if (Build.VERSION.SDK_INT >= 26) {
            if (needsForeground) {
                requireContext().startForegroundService(intent);
            } else {
                // Non-foreground actions: avoid startForeground timeout risk
                requireContext().startService(intent);
            }
        } else {
            requireContext().startService(intent);
        }
    }
    
    /**
     * Get player reference from PlaybackStateManager.
     * Updates local player field.
     */
    protected void getPlayerReference() {
        try {
            player = PlaybackStateManager.getInstance().getPlayer();
            if (player != null) {
                Log.d(getFragmentTag(), "Got player reference successfully");
            } else {
                Log.w(getFragmentTag(), "Player reference is null");
            }
        } catch (Exception e) {
            Log.e(getFragmentTag(), "Error getting player reference", e);
            player = null;
        }
    }
    
    // ==================== VALIDATION HELPERS ====================
    
    /**
     * Show error message on TextInputLayout.
     */
    protected void showError(TextInputLayout layout, String msg) {
        if (layout != null) {
            layout.setError(msg);
        }
    }
    
    /**
     * Clear error message from TextInputLayout.
     */
    protected void clearError(TextInputLayout layout) {
        if (layout != null) {
            layout.setError(null);
            layout.setErrorEnabled(false);
        }
    }
    
    /**
     * Safely parse integer from AutoCompleteTextView.
     * 
     * @param edit The AutoCompleteTextView to parse
     * @return Parsed integer, or -1 if parsing fails
     */
    protected int parseIntSafe(android.widget.AutoCompleteTextView edit) {
        try {
            String text = edit.getText() != null ? edit.getText().toString().trim() : "";
            return Integer.parseInt(text);
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Hide keyboard from view.
     */
    protected void hideKeyboard(View view) {
        try {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) 
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ignored) {
            // Keyboard hiding is not critical - ignore failures
        }
    }
    
    /**
     * Check if at least one reciter is selected.
     * Shows toast if no reciter is selected.
     * 
     * @return true if reciter is selected, false otherwise
     */
    protected boolean validateReciterSelection() {
        String savedOrder = requireContext()
            .getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
            .getString("reciters.order", "");
        
        if (savedOrder == null || savedOrder.trim().isEmpty()) {
            android.widget.Toast.makeText(requireContext(), 
                "Select at least one reciter first", 
                android.widget.Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
```

### Step 1.2: Understand What Stays in Subclasses

Each fragment will keep **only its unique logic**:

**VerseTabFragment keeps:**
- Surah/Ayah dropdown setup
- Custom ayah filtering with real-time validation
- Surah-specific validation logic
- `loadAndPlay()` implementation for single verse

**RangeTabFragment keeps:**
- Start/End surah/ayah dropdowns
- Smart auto-sync logic (`lastStartSurah`)
- Range validation (`isStartBeforeOrEqual`)
- UI visibility workaround (`ensureUIElementsVisible`)
- `loadAndPlay()` implementation for range

**SurahTabFragment keeps:**
- Surah dropdown setup (non-filterable)
- Simple surah validation
- `loadAndPlay()` implementation for full surah

**PageTabFragment keeps:**
- Page dropdown with smart filtering
- Page validation (1-604)
- `loadAndPlay()` implementation for page

---

## Phase 2: Migrate VerseTabFragment

### Step 2.1: Analysis - What Moves vs What Stays

**Moving to Base (already done):**
- ✅ Fields: `player`, `playPauseButton`, `isCurrentlyPlaying`, `reenableAtMs`, `justStopped`
- ✅ Methods: `sendService`, `showError`, `clearError`, `parseIntSafe`, `hideKeyboard`, `getPlayerReference`
- ✅ Lifecycle: `onResume`, `onPause`, `onDestroy`, `onDestroyView`
- ✅ Button logic: `handlePlayPauseToggle`, `handleStopButton`, `onPlaybackStateChanged`

**Staying in VerseTab:**
- Dropdown setup: `setupUi`, `setupAyahDropdown`
- Real-time validation: `TextWatcher` for ayah field
- Verse-specific loading: `loadAndPlayVerse`
- Ayah count data: `getAyahCount`, `AYAH_COUNTS`
- Test environment: `setupTestEnvironment`
- Content ownership: `isContentForThisFragment` (checks for "single")

### Step 2.2: Create New VerseTabFragment

**File:** `app/src/main/java/com/repeatquran/ui/VerseTabFragment.java` (modified)

```java
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
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.repeatquran.R;
import com.repeatquran.playback.PlaybackService;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for single verse (ayah) playback.
 * 
 * Features:
 * - Search-as-you-type surah selection
 * - Real-time ayah validation
 * - Smart ayah dropdown that updates when surah changes
 */
public class VerseTabFragment extends BaseTabFragment {
    
    @Override
    protected String getFragmentTag() {
        return "VerseTabFragment";
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                            @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        Log.d(getFragmentTag(), "onCreateView called");
        View v = inflater.inflate(R.layout.fragment_verse_tab, container, false);
        setupUi(v);
        Log.d(getFragmentTag(), "onCreateView completed");
        return v;
    }
    
    private void setupUi(View root) {
        Log.d(getFragmentTag(), "setupUi called");
        
        // Setup dropdowns
        AutoCompleteTextView ddSurah = root.findViewById(R.id.surahDropdown);
        TextInputLayout surahLayout = root.findViewById(R.id.surahInputLayout);
        TextInputLayout ayahLayout = root.findViewById(R.id.ayahInputLayout);
        AutoCompleteTextView ddAyah = root.findViewById(R.id.ayahDropdown);
        
        String[] display = com.repeatquran.util.SurahNames.displayList();
        
        // Search-as-you-type adapter for Surah selection
        com.repeatquran.ui.adapters.SurahAutoCompleteAdapter surahAdapter =
                new com.repeatquran.ui.adapters.SurahAutoCompleteAdapter(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, display);
        ddSurah.setAdapter(surahAdapter);
        ddSurah.setThreshold(1); // Enable filtering after 1 character
        ddSurah.setDropDownHeight(android.widget.ListPopupWindow.WRAP_CONTENT);
        
        // Prefill last surah if available
        int lastSurah = requireContext().getSharedPreferences("rq_prefs", 
            requireContext().MODE_PRIVATE).getInt("last.surah.single", 1);
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
        });
        
        // Setup common buttons (Play/Pause, Stop) - inherited from base
        setupCommonButtons(root);
        
        // Test-only helper to seed a reciter
        setupTestEnvironment();
    }
    
    @Override
    protected void loadAndPlay() {
        View root = getView();
        if (root == null) return;
        
        AutoCompleteTextView ddSurah = root.findViewById(R.id.surahDropdown);
        TextInputLayout surahLayout = root.findViewById(R.id.surahInputLayout);
        TextInputLayout ayahLayout = root.findViewById(R.id.ayahInputLayout);
        AutoCompleteTextView ddAyah = root.findViewById(R.id.ayahDropdown);
        
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
        int repeat = requireContext().getSharedPreferences("rq_prefs", 
            requireContext().MODE_PRIVATE).getInt("repeat.count", 1);
        
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
                "Loading Surah " + String.format("%03d", surah) + " — " + 
                com.repeatquran.util.SurahNames.name(surah) +
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
            
            // This fragment handles "single" ayah content
            boolean isSingleType = "single".equals(sourceType);
            Log.d(getFragmentTag(), "Content validation: sourceType=" + 
                  sourceType + ", isSingle=" + isSingleType);
            
            return isSingleType;
        } catch (Exception e) {
            Log.e(getFragmentTag(), "Error checking content ownership", e);
            return false;
        }
    }
    
    // ==================== VERSE-SPECIFIC HELPERS ====================
    
    private void setupAyahDropdown(AutoCompleteTextView ddAyah, 
                                   TextInputLayout ayahLayout, 
                                   int surahNumber) {
        int maxAyah = getAyahCount(surahNumber);
        
        // Create list of ayah numbers
        List<String> ayahNumbers = new ArrayList<>();
        for (int i = 1; i <= maxAyah; i++) {
            ayahNumbers.add(String.valueOf(i));
        }
        
        // Create adapter with filtering support
        ArrayAdapter<String> ayahAdapter = new ArrayAdapter<String>(requireContext(), 
            android.R.layout.simple_dropdown_item_1line, ayahNumbers) {
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
    
    private void setupTestEnvironment() {
        try {
            // In debug builds only, seed one reciter to ease local testing
            if (android.os.Build.FINGERPRINT != null && 
                android.os.Build.FINGERPRINT.contains("robolectric")) {
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
```

### Step 2.3: Test VerseTabFragment

**Testing Checklist:**

```bash
# 1. Compile check
./gradlew compileDebugSources

# 2. Manual test scenarios:
```

**Test Case 1: Basic Loading**
- [ ] Open Verse tab
- [ ] Select Surah 002 — Al-Baqarah
- [ ] Select Ayah 255
- [ ] Press Play
- [ ] **Expected:** Audio loads and plays
- [ ] **Verify:** Button shows "Pause"

**Test Case 2: Play/Pause Toggle**
- [ ] While playing, press Pause
- [ ] **Expected:** Audio pauses
- [ ] **Verify:** Button shows "Play"
- [ ] Press Play again
- [ ] **Expected:** Audio resumes
- [ ] **Verify:** Button shows "Pause"

**Test Case 3: Stop Button**
- [ ] While playing, press Stop
- [ ] **Expected:** Audio stops immediately
- [ ] **Verify:** Button shows "Play"
- [ ] **Verify:** No flicker (justStopped flag working)

**Test Case 4: Tab Switching**
- [ ] Load and play Verse 2:255
- [ ] Switch to Range tab
- [ ] Switch back to Verse tab
- [ ] **Expected:** Button shows "Pause" (if still playing)
- [ ] Press Pause
- [ ] **Expected:** Verse pauses correctly

**Test Case 5: Validation**
- [ ] Try to play without selecting reciter
- [ ] **Expected:** Toast "Select at least one reciter first"
- [ ] Select invalid ayah (e.g., 999 for Al-Fatihah)
- [ ] **Expected:** Red border + error "Ayah 1..7"
- [ ] Enter valid values
- [ ] **Expected:** Errors clear

**Test Case 6: Keyboard Hiding**
- [ ] Open surah dropdown
- [ ] Select a surah
- [ ] **Expected:** Keyboard hides automatically
- [ ] Dropdown dismisses

### Step 2.4: Commit VerseTabFragment Migration

```bash
git add app/src/main/java/com/repeatquran/ui/BaseTabFragment.java
git add app/src/main/java/com/repeatquran/ui/VerseTabFragment.java
git commit -m "refactor: Extract BaseTabFragment + migrate VerseTabFragment

- Create BaseTabFragment with common fragment logic
- Move ~250 lines of duplicate code to base class
- Migrate VerseTabFragment to extend BaseTabFragment
- Reduce VerseTabFragment from 506 to ~280 lines
- Add onDestroyView cleanup to prevent memory leaks

BREAKING: None - fully backward compatible
TESTED: All VerseTab functionality verified"
```

---

## Phase 3: Migrate RangeTabFragment

### Step 3.1: Analysis - RangeTab Specifics

**Unique to RangeTab:**
- 4 dropdowns (start/end surah, start/end ayah)
- Smart auto-sync (`lastStartSurah` tracking)
- Range validation (`isStartBeforeOrEqual`)
- UI visibility workaround (`ensureUIElementsVisible`)
- More complex state (6 fields vs 2-4 in others)

**Moving to Base:** (already done)
- All common button/playback logic

**Staying in RangeTab:**
- Dropdown setup for 4 fields
- Auto-sync logic
- Range validation
- UI workaround
- Range-specific loading

### Step 3.2: Create New RangeTabFragment

**File:** `app/src/main/java/com/repeatquran/ui/RangeTabFragment.java` (modified)

```java
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

/**
 * Fragment for range playback (start ayah → end ayah).
 * 
 * Features:
 * - Start/End surah and ayah selection
 * - Smart auto-sync: End surah copies Start if empty or equal to previous Start
 * - Range validation: ensures end >= start
 * - UI visibility workaround for Android layout bug
 */
public class RangeTabFragment extends BaseTabFragment {
    
    // Range-specific state
    private int lastStartSurah = -1; // Track previous start surah for smart auto-sync
    
    @Override
    protected String getFragmentTag() {
        return "RangeTabFragment";
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                            @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_range_tab, container, false);
        setupUi(v);
        return v;
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
    
    private void setupUi(View root) {
        // Setup dropdowns
        AutoCompleteTextView ddStart = root.findViewById(R.id.startSurahDropdown);
        AutoCompleteTextView ddEnd = root.findViewById(R.id.endSurahDropdown);
        TextInputLayout startSurahLayout = root.findViewById(R.id.startSurahLayout);
        TextInputLayout endSurahLayout = root.findViewById(R.id.endSurahLayout);
        TextInputLayout startAyahLayout = root.findViewById(R.id.startAyahLayout);
        TextInputLayout endAyahLayout = root.findViewById(R.id.endAyahLayout);
        AutoCompleteTextView ddStartAyah = root.findViewById(R.id.startAyahDropdown);
        AutoCompleteTextView ddEndAyah = root.findViewById(R.id.endAyahDropdown);
        
        String[] display = com.repeatquran.util.SurahNames.displayList();
        
        // Search-as-you-type adapters
        com.repeatquran.ui.adapters.SurahAutoCompleteAdapter startAdapter =
                new com.repeatquran.ui.adapters.SurahAutoCompleteAdapter(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, display);
        ddStart.setAdapter(startAdapter);
        ddStart.setThreshold(1);
        ddStart.setDropDownHeight(android.widget.ListPopupWindow.WRAP_CONTENT);
        
        com.repeatquran.ui.adapters.SurahAutoCompleteAdapter endAdapter =
                new com.repeatquran.ui.adapters.SurahAutoCompleteAdapter(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, display);
        ddEnd.setAdapter(endAdapter);
        ddEnd.setThreshold(1);
        ddEnd.setDropDownHeight(android.widget.ListPopupWindow.WRAP_CONTENT);
        
        // Restore last values
        android.content.SharedPreferences prefsRange = requireContext()
            .getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE);
        int lastStart = prefsRange.getInt("last.surah.range.start", 1);
        int lastEnd = prefsRange.getInt("last.surah.range.end", 1);
        
        // Initialize start surah
        if (lastStart >= 1 && lastStart <= 114) {
            ddStart.setText(com.repeatquran.util.SurahNames.display(lastStart), false);
            setupAyahDropdown(ddStartAyah, startAyahLayout, lastStart);
            lastStartSurah = lastStart; // Track initial start surah
        }
        
        // Initialize end surah - apply auto-sync logic even on initial load
        if (lastEnd >= 1 && lastEnd <= 114) {
            ddEnd.setText(com.repeatquran.util.SurahNames.display(lastEnd), false);
            setupAyahDropdown(ddEndAyah, endAyahLayout, lastEnd);
        } else if (lastStart >= 1 && lastStart <= 114) {
            // If no end surah saved, default to start surah
            ddEnd.setText(com.repeatquran.util.SurahNames.display(lastStart), false);
            setupAyahDropdown(ddEndAyah, endAyahLayout, lastStart);
        }
        
        // Restore last selected ayah values
        int lastStartAyah = prefsRange.getInt("last.ayah.range.start", 1);
        int lastEndAyah = prefsRange.getInt("last.ayah.range.end", 1);
        if (lastStartAyah > 0) ddStartAyah.setText(String.valueOf(lastStartAyah), false);
        if (lastEndAyah > 0) ddEndAyah.setText(String.valueOf(lastEndAyah), false);
        
        // Ensure all UI elements are visible initially
        ensureUIElementsVisible(root);
        
        // Start surah selection with smart auto-sync
        ddStart.setOnItemClickListener((parent, v, pos, id) -> {
            int newStartSurah = parseSurahFromSelection(parent, pos, ddStart);
            if (newStartSurah < 1 || newStartSurah > 114) return;
            
            setupAyahDropdown(ddStartAyah, startAyahLayout, newStartSurah);
            
            // Smart auto-sync: Copy Start → End if End is empty or End equals old Start
            String currentEndText = ddEnd.getText() != null ? 
                ddEnd.getText().toString().trim() : "";
            int currentEndSurah = parseSurahFromText(currentEndText);
            
            // Auto-set End Surah if: empty OR equals previous Start Surah
            if (currentEndSurah == -1 || currentEndSurah == lastStartSurah) {
                ddEnd.setText(com.repeatquran.util.SurahNames.display(newStartSurah), false);
                setupAyahDropdown(ddEndAyah, endAyahLayout, newStartSurah);
                Log.d(getFragmentTag(), "Auto-synced End Surah to " + newStartSurah);
            }
            
            // Update tracker
            lastStartSurah = newStartSurah;
            
            // Hide keyboard and refresh UI
            ddStart.dismissDropDown();
            hideKeyboard(ddStart);
            ddStart.clearFocus();
            if (root != null) root.requestFocus();
            ensureUIElementsVisible(root);
        });
        
        // End surah selection
        ddEnd.setOnItemClickListener((parent, v, pos, id) -> {
            int surahNumber = parseSurahFromSelection(parent, pos, ddEnd);
            if (surahNumber < 1 || surahNumber > 114) return;
            
            setupAyahDropdown(ddEndAyah, endAyahLayout, surahNumber);
            ddEnd.dismissDropDown();
            hideKeyboard(ddEnd);
            ddEnd.clearFocus();
            if (root != null) root.requestFocus();
            ensureUIElementsVisible(root);
        });
        
        // Setup common buttons (Play/Pause, Stop) - inherited from base
        setupCommonButtons(root);
    }
    
    @Override
    protected void loadAndPlay() {
        View root = getView();
        if (root == null) return;
        
        AutoCompleteTextView ddStart = root.findViewById(R.id.startSurahDropdown);
        AutoCompleteTextView ddEnd = root.findViewById(R.id.endSurahDropdown);
        TextInputLayout startSurahLayout = root.findViewById(R.id.startSurahLayout);
        TextInputLayout endSurahLayout = root.findViewById(R.id.endSurahLayout);
        TextInputLayout startAyahLayout = root.findViewById(R.id.startAyahLayout);
        TextInputLayout endAyahLayout = root.findViewById(R.id.endAyahLayout);
        AutoCompleteTextView ddStartAyah = root.findViewById(R.id.startAyahDropdown);
        AutoCompleteTextView ddEndAyah = root.findViewById(R.id.endAyahDropdown);
        
        clearError(startSurahLayout);
        clearError(endSurahLayout);
        clearError(startAyahLayout);
        clearError(endAyahLayout);
        
        // Validate reciter selection
        if (!validateReciterSelection()) return;
        
        // Parse and validate start surah
        String s1 = ddStart.getText() != null ? ddStart.getText().toString().trim() : "";
        if (s1.length() < 3) {
            showError(startSurahLayout, "Select start");
            return;
        }
        
        int ss;
        try {
            ss = Integer.parseInt(s1.substring(0, 3));
        } catch (Exception e) {
            showError(startSurahLayout, "Select start");
            return;
        }
        
        if (ss < 1 || ss > 114) {
            showError(startSurahLayout, "1..114");
            return;
        }
        
        // Parse and validate end surah
        String s2 = ddEnd.getText() != null ? ddEnd.getText().toString().trim() : "";
        if (s2.length() < 3) {
            showError(endSurahLayout, "Select end");
            return;
        }
        
        int es;
        try {
            es = Integer.parseInt(s2.substring(0, 3));
        } catch (Exception e) {
            showError(endSurahLayout, "Select end");
            return;
        }
        
        if (es < 1 || es > 114) {
            showError(endSurahLayout, "1..114");
            return;
        }
        
        // Parse and validate ayahs
        int sa = parseIntSafe(ddStartAyah);
        int ea = parseIntSafe(ddEndAyah);
        
        if (sa < 1 || sa > getAyahCount(ss)) {
            showError(startAyahLayout, "Ayah 1.." + getAyahCount(ss));
            return;
        }
        
        if (ea < 1 || ea > getAyahCount(es)) {
            showError(endAyahLayout, "Ayah 1.." + getAyahCount(es));
            return;
        }
        
        // Validate range logic
        if (!isStartBeforeOrEqual(ss, sa, es, ea)) {
            showError(endSurahLayout, "End before start");
            showError(endAyahLayout, "End before start");
            return;
        }
        
        // Persist values
        requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE)
                .edit()
                .putInt("last.surah.range.start", ss)
                .putInt("last.surah.range.end", es)
                .putInt("last.ayah.range.start", sa)
                .putInt("last.ayah.range.end", ea)
                .apply();
        
        // Get settings
        int repeat = requireContext().getSharedPreferences("rq_prefs", 
            requireContext().MODE_PRIVATE).getInt("repeat.count", 1);
        boolean half = requireContext().getSharedPreferences("rq_prefs", 
            requireContext().MODE_PRIVATE).getBoolean("ui.half.split", false);
        
        // Create intent
        Intent intent = new Intent(requireContext(), PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_LOAD_RANGE);
        intent.putExtra("ss", ss);
        intent.putExtra("sa", sa);
        intent.putExtra("es", es);
        intent.putExtra("ea", ea);
        intent.putExtra("repeat", repeat);
        intent.putExtra("halfSplit", half);
        
        // Send to service
        sendService(PlaybackService.ACTION_LOAD_RANGE, intent);
        
        // Show feedback
        android.widget.Toast.makeText(requireContext(), 
            "Loading range…", android.widget.Toast.LENGTH_SHORT).show();
        
        // Set loading state (1.2s cooldown)
        setButtonLoadingState(1200);
    }
    
    @Override
    protected boolean isContentForThisFragment() {
        try {
            android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE);
            String sourceType = prefs.getString("resume.sourceType", "");
            
            // This fragment handles "range" content
            boolean isRangeType = "range".equals(sourceType);
            Log.d(getFragmentTag(), "Content validation: sourceType=" + 
                  sourceType + ", isRange=" + isRangeType);
            
            return isRangeType;
        } catch (Exception e) {
            Log.e(getFragmentTag(), "Error checking content ownership", e);
            return false;
        }
    }
    
    // ==================== RANGE-SPECIFIC HELPERS ====================
    
    /**
     * Parse surah number from dropdown selection.
     * Handles filtered results correctly.
     */
    private int parseSurahFromSelection(android.widget.AdapterView<?> parent, 
                                       int pos, 
                                       AutoCompleteTextView dropdown) {
        try {
            Object clicked = parent != null ? parent.getItemAtPosition(pos) : null;
            String label = clicked != null ? String.valueOf(clicked) : 
                (dropdown.getText() != null ? dropdown.getText().toString().trim() : "");
            return parseSurahFromText(label);
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Parse surah number from text (e.g., "002 — Al-Baqarah" → 2).
     */
    private int parseSurahFromText(String text) {
        if (text == null || text.length() < 3) return -1;
        
        try {
            // Try extracting first 3 chars
            return Integer.parseInt(text.substring(0, 3));
        } catch (Exception e1) {
            // Fallback: extract leading digits
            try {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < text.length() && Character.isDigit(text.charAt(i)) 
                     && sb.length() < 3; i++) {
                    sb.append(text.charAt(i));
                }
                if (sb.length() > 0) {
                    return Integer.parseInt(sb.toString());
                }
            } catch (Exception e2) {
                // Give up
            }
        }
        return -1;
    }
    
    /**
     * Validate that start position is before or equal to end position.
     */
    private boolean isStartBeforeOrEqual(int ss, int sa, int es, int ea) {
        if (ss < es) return true;
        if (ss > es) return false;
        return sa <= ea;
    }
    
    /**
     * Setup ayah dropdown for a given surah.
     * Preserves current value if still valid.
     */
    private void setupAyahDropdown(AutoCompleteTextView ddAyah, 
                                   TextInputLayout ayahLayout, 
                                   int surahNumber) {
        int maxAyah = getAyahCount(surahNumber);
        
        // Capture current value to preserve if still valid
        String currentText = ddAyah.getText() != null ? 
            ddAyah.getText().toString().trim() : "";
        int currentVal = -1;
        try {
            currentVal = Integer.parseInt(currentText);
        } catch (Exception ignored) {}
        
        // Create list of ayah numbers
        java.util.List<String> ayahNumbers = new java.util.ArrayList<>();
        for (int i = 1; i <= maxAyah; i++) {
            ayahNumbers.add(String.valueOf(i));
        }
        
        // Create adapter
        ArrayAdapter<String> ayahAdapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_dropdown_item_1line, ayahNumbers);
        ddAyah.setAdapter(ayahAdapter);
        ddAyah.setThreshold(Integer.MAX_VALUE); // Disable text filtering
        ddAyah.setDropDownHeight(android.widget.ListPopupWindow.WRAP_CONTENT);
        
        // When ayah is selected, dismiss keyboard
        ddAyah.setOnItemClickListener((p, v, pos, id) -> {
            ddAyah.dismissDropDown();
            hideKeyboard(ddAyah);
            ddAyah.clearFocus();
            View rootView = getView();
            if (rootView != null) rootView.requestFocus();
        });
        
        // Decide what value to show after surah change
        String newText;
        if (currentVal >= 1 && currentVal <= maxAyah) {
            newText = String.valueOf(currentVal); // Preserve current
        } else if (currentVal > maxAyah) {
            newText = String.valueOf(maxAyah); // Clamp to max
        } else {
            newText = "1"; // Default
        }
        
        ddAyah.setText(newText, false);
        ayahLayout.setHelperText("Max ayah: " + maxAyah);
        
        // Clear error if value is now valid
        try {
            int nv = Integer.parseInt(newText);
            if (nv >= 1 && nv <= maxAyah) {
                clearError(ayahLayout);
            }
        } catch (Exception ignored) {}
    }
    
    private int getAyahCount(int surah) {
        return com.repeatquran.util.AyahCounts.getCount(surah);
    }
    
    /**
     * Workaround for Android layout bug where End Ayah and Play buttons disappear.
     * Forces visibility and layout refresh.
     */
    private void ensureUIElementsVisible(View root) {
        View endAyahLayout = root.findViewById(R.id.endAyahLayout);
        View endSurahLayout = root.findViewById(R.id.endSurahLayout);
        View btnPlayPause = root.findViewById(R.id.btnPlayPause);
        View btnStop = root.findViewById(R.id.btnStop);
        
        if (endAyahLayout != null) {
            endAyahLayout.setVisibility(View.VISIBLE);
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
}
```

### Step 3.3: Test RangeTabFragment

**Testing Checklist:**

**Test Case 1: Basic Range Loading**
- [ ] Open Range tab
- [ ] Select Start: 002 — Al-Baqarah, Ayah 1
- [ ] Select End: 002 — Al-Baqarah, Ayah 10
- [ ] Press Play
- [ ] **Expected:** Range loads and plays
- [ ] **Verify:** Plays 10 verses (2:1 through 2:10)

**Test Case 2: Smart Auto-Sync**
- [ ] Select Start: 003 — Aal Imran
- [ ] **Expected:** End auto-updates to 003 — Aal Imran
- [ ] **Verify:** End dropdown shows "003 — Aal Imran"
- [ ] Change Start to 004 — An-Nisa
- [ ] **Expected:** End auto-updates to 004 — An-Nisa
- [ ] Manually change End to 005 — Al-Maidah
- [ ] Change Start to 006 — Al-An'am
- [ ] **Expected:** End stays 005 (no auto-sync)

**Test Case 3: Range Validation**
- [ ] Select Start: 002:100
- [ ] Select End: 002:50
- [ ] Press Play
- [ ] **Expected:** Error "End before start"
- [ ] Fix: Change End to 002:150
- [ ] **Expected:** Error clears, range loads

**Test Case 4: UI Visibility**
- [ ] Select Start surah from dropdown
- [ ] **Expected:** End Ayah field still visible
- [ ] **Verify:** Play button still visible
- [ ] Select End surah from dropdown
- [ ] **Expected:** All elements still visible

**Test Case 5: State Persistence**
- [ ] Load range 2:1 → 2:50
- [ ] Close app
- [ ] Reopen app, go to Range tab
- [ ] **Expected:** Dropdowns show 2:1 → 2:50

### Step 3.4: Commit RangeTabFragment Migration

```bash
git add app/src/main/java/com/repeatquran/ui/RangeTabFragment.java
git commit -m "refactor: Migrate RangeTabFragment to BaseTabFragment

- Extend BaseTabFragment for common functionality
- Reduce RangeTabFragment from 574 to ~350 lines
- Preserve smart auto-sync logic
- Preserve UI visibility workaround
- Keep range-specific validation

BREAKING: None - fully backward compatible
TESTED: All RangeTab functionality verified"
```

---

## Phase 4: Migrate SurahTabFragment

*[Abbreviated - follows same pattern as VerseTab/RangeTab]*

**Key Points:**
- Simplest migration (fewest unique features)
- Keep non-filterable dropdown behavior
- Reduce from 321 to ~100 lines

**Commit:**
```bash
git commit -m "refactor: Migrate SurahTabFragment to BaseTabFragment

- Reduce from 321 to ~100 lines (69% reduction)
- Preserve non-filterable dropdown (intentional UX)
- Simplest migration due to minimal unique logic"
```

---

## Phase 5: Migrate PageTabFragment

*[Abbreviated - follows same pattern]*

**Key Points:**
- Keep smart page dropdown with "common pages"
- Keep custom filter for 604 pages
- Reduce from 385 to ~130 lines

**Commit:**
```bash
git commit -m "refactor: Migrate PageTabFragment to BaseTabFragment

- Reduce from 385 to ~130 lines (66% reduction)
- Preserve smart page dropdown UX
- All 4 fragments now extend BaseTabFragment"
```

---

## Phase 6: Add State Persistence

### Step 6.1: Add State Persistence to BaseTabFragment

**Goal:** Implement savedInstanceState handling **once** in BaseTabFragment

**Add to BaseTabFragment.java:**

```java
// ==================== STATE PERSISTENCE ====================

/**
 * Save fragment-specific UI state.
 * Subclasses must implement to save their dropdown values.
 * 
 * Example:
 * outState.putString("surah", ddSurah.getText().toString());
 */
protected abstract void onSaveFragmentState(@NonNull Bundle outState);

/**
 * Restore fragment-specific UI state.
 * Subclasses must implement to restore their dropdown values.
 * 
 * Example:
 * String surah = savedInstanceState.getString("surah");
 * if (surah != null) ddSurah.setText(surah, false);
 */
protected abstract void onRestoreFragmentState(@NonNull Bundle savedInstanceState);

@Override
public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    
    // Save common state
    outState.putBoolean("isCurrentlyPlaying", isCurrentlyPlaying);
    outState.putLong("reenableAtMs", reenableAtMs);
    outState.putBoolean("justStopped", justStopped);
    
    // Let subclass save its specific state
    onSaveFragmentState(outState);
    
    Log.d(getFragmentTag(), "onSaveInstanceState: saved common + fragment state");
}

@Override
public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    
    if (savedInstanceState != null) {
        // Restore common state
        isCurrentlyPlaying = savedInstanceState.getBoolean("isCurrentlyPlaying", false);
        reenableAtMs = savedInstanceState.getLong("reenableAtMs", 0L);
        justStopped = savedInstanceState.getBoolean("justStopped", false);
        
        // Update button UI
        updateButtonUI(isCurrentlyPlaying);
        
        // Let subclass restore its specific state
        onRestoreFragmentState(savedInstanceState);
        
        Log.d(getFragmentTag(), "onViewStateRestored: restored common + fragment state");
    }
}
```

### Step 6.2: Implement State Persistence in VerseTabFragment

**Add to VerseTabFragment.java:**

```java
private AutoCompleteTextView ddSurah;
private AutoCompleteTextView ddAyah;

// Update setupUi to save references:
private void setupUi(View root) {
    ddSurah = root.findViewById(R.id.surahDropdown);
    ddAyah = root.findViewById(R.id.ayahDropdown);
    // ... rest of setup
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
```

### Step 6.3: Implement for Other Fragments

**RangeTabFragment:**
- Save/restore 4 dropdown values + `lastStartSurah`

**SurahTabFragment:**
- Save/restore 1 dropdown value

**PageTabFragment:**
- Save/restore 1 dropdown value

### Step 6.4: Test State Persistence

**Critical Test:**
```bash
# For EACH tab:
1. Fill in ALL form fields
2. DO NOT press Play
3. Rotate device (Ctrl+F11 in emulator)
4. Verify: All fields preserved
5. Press Play
6. Verify: Loads correctly with preserved values
```

### Step 6.5: Commit State Persistence

```bash
git add app/src/main/java/com/repeatquran/ui/BaseTabFragment.java
git add app/src/main/java/com/repeatquran/ui/*TabFragment.java
git commit -m "feat: Add state persistence to all tab fragments

- Implement onSaveInstanceState in BaseTabFragment
- Add abstract methods for fragment-specific state
- Implement state save/restore in all 4 fragments
- User input now preserved across rotation

FIXES: #[issue number] - Input lost on rotation
TESTED: Rotation test passed on all 4 tabs"
```

---

## Testing Strategy

### Unit Tests (Optional but Recommended)

**Create:** `app/src/test/java/com/repeatquran/ui/BaseTabFragmentTest.java`

```java
@RunWith(RobolectricTestRunner.class)
public class BaseTabFragmentTest {
    
    @Test
    public void testPlayPauseToggle_NoContent_LoadsNew() {
        // Arrange: Fragment with no content
        // Act: Press Play
        // Assert: loadAndPlay() called
    }
    
    @Test
    public void testPlayPauseToggle_Playing_Pauses() {
        // Arrange: Fragment with playing content
        // Act: Press Pause
        // Assert: Service receives ACTION_PAUSE
    }
    
    @Test
    public void testStopButton_ResetsState() {
        // Arrange: Fragment with playing content
        // Act: Press Stop
        // Assert: isCurrentlyPlaying = false, button = "Play"
    }
    
    @Test
    public void testCooldown_PreventsRapidClicks() {
        // Arrange: Set cooldown
        // Act: Click button rapidly
        // Assert: Only first click processes
    }
}
```

### Integration Tests

**Manual Checklist:**

```
Phase 1: Individual Fragment Tests
□ VerseTab: All functionality works
□ RangeTab: All functionality works
□ SurahTab: All functionality works
□ PageTab: All functionality works

Phase 2: Cross-Tab Tests
□ Switch tabs while playing → state preserved
□ Load in one tab, switch, return → correct state
□ Stop in one tab, switch to another, play → new content

Phase 3: Rotation Tests
□ Rotate with form filled (not played) → data preserved
□ Rotate while playing → playback continues
□ Rotate after stop → state correct

Phase 4: Edge Cases
□ Rapid button clicking → no crashes
□ Switch tabs during loading → no crashes
□ Rotate during loading → no crashes
□ Stop during loading → loading cancels
```

---

## Rollback Plan

### If Issues Arise During Migration

**Scenario 1: VerseTab broken after migration**
```bash
git revert HEAD  # Revert VerseTab commit
# Fix issues
# Re-commit
```

**Scenario 2: Multiple fragments broken**
```bash
git revert HEAD~3..HEAD  # Revert last 3 commits
# Start over with fixes
```

**Scenario 3: Critical bug in production**
```bash
git revert <commit-hash>  # Revert specific problematic commit
# All other migrations remain intact
```

### Safety Measures

1. **Commit after each fragment migration** (not all at once)
2. **Test each fragment before moving to next**
3. **Keep old code commented in first draft** (can uncomment if needed)
4. **Tag releases:** `git tag v1.0-before-refactor`

---

## Summary

### What We Built

1. **BaseTabFragment** (~250 lines)
   - Common playback logic
   - State management
   - Service communication
   - Validation helpers
   - State persistence (Step 6)

2. **Refactored Fragments** (~580 lines total, down from ~1,786)
   - VerseTabFragment: 506 → ~280 lines
   - RangeTabFragment: 574 → ~350 lines
   - SurahTabFragment: 321 → ~100 lines
   - PageTabFragment: 385 → ~130 lines

**Total Lines Removed:** ~1,206 lines  
**Total Lines Added:** ~830 lines (Base + refactored)  
**Net Reduction:** ~376 lines (~21% smaller codebase)

### Benefits Achieved

✅ **Single source of truth** for common logic  
✅ **Bug fixes propagate automatically** to all fragments  
✅ **State persistence implemented once** (all fragments inherit)  
✅ **Easier to maintain** (4x less code to update)  
✅ **Memory leak fixed** (onDestroyView cleanup in base)  
✅ **Consistent patterns** across all fragments  

### Time Estimate

- **Phase 1 (BaseTabFragment):** 2 hours
- **Phase 2 (VerseTab):** 1.5 hours
- **Phase 3 (RangeTab):** 2 hours (complex)
- **Phase 4 (SurahTab):** 0.5 hours (simple)
- **Phase 5 (PageTab):** 1 hour
- **Phase 6 (State Persistence):** 2 hours
- **Testing:** 2 hours

**Total:** ~11 hours (1.5 days)

---

## Next Steps

After completing this refactoring, we can tackle:

1. ✅ **State persistence** (already done in Phase 6)
2. ✅ **Memory leak prevention** (already done via onDestroyView)
3. **Standardize dropdown behavior** (make all filterable)
4. **Fix RangeTab layout issue** (investigate root cause)
5. **Add network error handling**
6. **Implement loading states**

---

**End of Implementation Plan**

*Ready to execute? Start with Phase 1: Create BaseTabFragment!*
