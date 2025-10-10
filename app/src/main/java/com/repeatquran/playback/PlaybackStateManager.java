package com.repeatquran.playback;

import androidx.annotation.Nullable;
import android.util.Log;
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
        Log.d("PlaybackStateManager", "setPlayer called with: " + (player != null ? "non-null player" : "null player"));
        
        if (this.player != null) {
            this.player.removeListener(playerListener);
            Log.d("PlaybackStateManager", "Removed listener from old player");
        }
        
        this.player = player;
        
        if (this.player != null) {
            this.player.addListener(playerListener);
            Log.d("PlaybackStateManager", "Added listener to new player");
            updateState();
        } else {
            hasQueue = false;
            isPlaying = false;
            Log.d("PlaybackStateManager", "Player is null, setting initial state");
            notifyListeners();
        }
    }
    
    public void addListener(StateChangeListener listener) {
        Log.d("PlaybackStateManager", "addListener called. Current listeners: " + listeners.size());
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Log.d("PlaybackStateManager", "Added listener. Total listeners: " + listeners.size());
            // Immediately notify with current state
            Log.d("PlaybackStateManager", "Immediately notifying new listener: hasQueue=" + hasQueue + ", isPlaying=" + isPlaying);
            listener.onPlaybackStateChanged(hasQueue, isPlaying);
        } else {
            Log.d("PlaybackStateManager", "Listener already exists, not adding");
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
        boolean oldHasQueue = hasQueue;
        boolean oldIsPlaying = isPlaying;
        
        if (player == null) {
            hasQueue = false;
            isPlaying = false;
        } else {
            hasQueue = player.getMediaItemCount() > 0;
            isPlaying = player.isPlaying();
        }
        
        Log.d("PlaybackStateManager", "updateState: hasQueue " + oldHasQueue + " -> " + hasQueue + ", isPlaying " + oldIsPlaying + " -> " + isPlaying);
        notifyListeners();
    }
    
    private void notifyListeners() {
        Log.d("PlaybackStateManager", "notifyListeners: " + listeners.size() + " listeners, hasQueue=" + hasQueue + ", isPlaying=" + isPlaying);
        for (StateChangeListener listener : new ArrayList<>(listeners)) {
            Log.d("PlaybackStateManager", "Notifying listener: " + listener.getClass().getSimpleName());
            listener.onPlaybackStateChanged(hasQueue, isPlaying);
        }
    }
    
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Log.d("PlaybackStateManager", "ExoPlayer onIsPlayingChanged: " + isPlaying);
            updateState();
        }
        
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            Log.d("PlaybackStateManager", "ExoPlayer onPlaybackStateChanged: " + playbackState);
            updateState();
        }
        
        @Override
        public void onMediaItemTransition(@Nullable com.google.android.exoplayer2.MediaItem mediaItem, int reason) {
            Log.d("PlaybackStateManager", "ExoPlayer onMediaItemTransition");
            updateState();
        }
    };
}