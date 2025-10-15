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
 * - onSaveFragmentState(): Save fragment-specific state
 * - onRestoreFragmentState(): Restore fragment-specific state
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
    
    // ==================== STATE PERSISTENCE ====================
    
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
            if (playPauseButton != null) {
                updateButtonUI(isCurrentlyPlaying);
            }
            
            // Let subclass restore its specific state
            onRestoreFragmentState(savedInstanceState);
            
            Log.d(getFragmentTag(), "onViewStateRestored: restored common + fragment state");
        }
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
