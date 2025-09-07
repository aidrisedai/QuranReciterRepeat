package com.repeatquran.playback;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;

/**
 * Minimal playback manager with lazy-fetch-next behavior and a small buffer ahead.
 */
public class PlaybackManager {
    public interface Callback {
        void onBufferAppended(int index);
        void onPlaybackError(String message);
        void onStateChanged(int state);
    }

    private static final String TAG = "PlaybackManager";

    private final VerseProvider provider;
    private final int bufferAhead;
    private final ExoPlayer player;
    private final Callback callback;

    private int nextToAppend = 0;
    private boolean feedingEnabled = true;

    public PlaybackManager(Context context, VerseProvider provider, int bufferAhead, @Nullable Callback callback) {
        this.provider = provider;
        this.bufferAhead = Math.max(0, bufferAhead);
        this.callback = callback;

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_SPEECH)
                .build();

        this.player = new ExoPlayer.Builder(context).build();
        this.player.setAudioAttributes(attrs, true);
        this.player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (callback != null) callback.onStateChanged(playbackState);
                maybeAppendMore();
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                maybeAppendMore();
            }

            @Override
            public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
                Log.e(TAG, "Playback error", error);
                if (callback != null) callback.onPlaybackError(error.getMessage());
            }
        });
    }

    public ExoPlayer getPlayer() { return player; }

    public void prepareAndStart() {
        if (provider.size() == 0) return;
        feedingEnabled = true;
        // Seed initial queue: current + bufferAhead
        int initial = Math.min(provider.size(), bufferAhead + 1);
        for (int i = 0; i < initial; i++) {
            appendNext();
        }
        player.prepare();
        player.play();
    }

    public void stop() {
        player.stop();
    }

    public void release() {
        player.release();
    }

    private void maybeAppendMore() {
        if (!feedingEnabled) return;
        int current = player.getCurrentMediaItemIndex();
        // If we're within the buffer window from the end of the appended items, append one more.
        if (nextToAppend < provider.size() && current >= nextToAppend - bufferAhead) {
            appendNext();
        }
    }

    private void appendNext() {
        if (nextToAppend >= provider.size()) return;
        String url = provider.urlAt(nextToAppend);
        MediaItem item = new MediaItem.Builder().setUri(url).build();
        player.addMediaItem(item);
        if (callback != null) callback.onBufferAppended(nextToAppend);
        Log.d(TAG, "Appended to buffer: index=" + nextToAppend);
        nextToAppend++;
    }

    /**
     * Enables or disables automatic feeding from the VerseProvider into the ExoPlayer queue.
     * When disabled, no more items are appended automatically.
     */
    public void setFeedingEnabled(boolean enabled) {
        this.feedingEnabled = enabled;
    }
}
