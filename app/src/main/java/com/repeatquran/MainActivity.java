package com.repeatquran;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.Player;
import com.repeatquran.playback.PlaybackManager;
import com.repeatquran.playback.SimpleVerseProvider;

public class MainActivity extends AppCompatActivity {
    private PlaybackManager playbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playbackManager = new PlaybackManager(
                this,
                new SimpleVerseProvider(),
                2,
                new PlaybackManager.Callback() {
                    @Override
                    public void onBufferAppended(int index) {
                        Log.d("MainActivity", "Buffer appended: " + index);
                    }

                    @Override
                    public void onPlaybackError(String message) {
                        Log.e("MainActivity", "Playback error: " + message);
                    }

                    @Override
                    public void onStateChanged(int state) {
                        Log.d("MainActivity", "State: " + state);
                    }
                }
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auto-start demo playback to facilitate proof capture for UHW-4.
        playbackManager.prepareAndStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        playbackManager.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playbackManager.release();
    }
}
