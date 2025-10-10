package com.repeatquran.playback;

import androidx.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple singleton to manage playback state and notify UI components directly
 * This replaces the complex broadcast mechanism with a simple observer pattern
 */
public class PlaybackStateManager {
    private static PlaybackStateManager instance;
    private List<StateChangeListener> listeners = new ArrayList<>();
    private ExoPlayer player;
    private boolean hasQueue = false;
    private boolean isPlaying = false;
    
    public interface StateChangeListener {
        void onPlaybackStateChanged(boolean hasQueue, boolean isPlaying);
    }
    
    public static synchronized PlaybackStateManager getInstance() {
        if (instance == null) {
            instance = new PlaybackStateManager();
        }
        return instance;
    }
    
    public void setPlayer(@Nullable ExoPlayer player) {
        if (this.player != null) {
            this.player.removeListener(playerListener);
        }
        
        this.player = player;
        
        if (this.player != null) {
            this.player.addListener(playerListener);
            updateState();
        } else {
            hasQueue = false;
            isPlaying = false;
            notifyListeners();
        }
    }
    
    public void addListener(StateChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            // Immediately notify with current state
            listener.onPlaybackStateChanged(hasQueue, isPlaying);
        }
    }
    
    public void removeListener(StateChangeListener listener) {
        listeners.remove(listener);
    }
    
    public boolean hasQueue() {
        return hasQueue;
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    private void updateState() {
        if (player == null) {
            hasQueue = false;
            isPlaying = false;
        } else {
            hasQueue = player.getMediaItemCount() > 0;
            isPlaying = player.isPlaying();
        }
        notifyListeners();
    }
    
    private void notifyListeners() {
        for (StateChangeListener listener : new ArrayList<>(listeners)) {
            listener.onPlaybackStateChanged(hasQueue, isPlaying);
        }
    }
    
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            updateState();
        }
        
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            updateState();
        }
        
        @Override
        public void onMediaItemTransition(@Nullable com.google.android.exoplayer2.MediaItem mediaItem, int reason) {
            updateState();
        }
    };
}