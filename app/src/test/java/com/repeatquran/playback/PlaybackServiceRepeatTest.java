package com.repeatquran.playback;

import static org.junit.Assert.assertEquals;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.repeatquran.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ServiceController;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
public class PlaybackServiceRepeatTest {

    @Test
    public void loadSingle_withRepeat3_enqueuesThreeItems() throws Exception {
        // Prepare prefs: select one reciter ID
        android.content.Context ctx = ApplicationProvider.getApplicationContext();
        String[] ids = ctx.getResources().getStringArray(R.array.reciter_ids);
        ctx.getSharedPreferences("rq_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("reciters.order", ids[0]).apply();

        ServiceController<PlaybackService> controller = Robolectric.buildService(PlaybackService.class).create().startCommand(0, 0);
        PlaybackService service = controller.get();

        Intent i = new Intent(ctx, PlaybackService.class);
        i.setAction(PlaybackService.ACTION_LOAD_SINGLE);
        i.putExtra("sura", 1);
        i.putExtra("ayah", 1);
        i.putExtra("repeat", 3);
        controller.withIntent(i).startCommand(0, 0);

        // Allow posted tasks to run
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        // Access private player via reflection
        Field f = PlaybackService.class.getDeclaredField("player");
        f.setAccessible(true);
        com.google.android.exoplayer2.ExoPlayer player = (com.google.android.exoplayer2.ExoPlayer) f.get(service);
        assertEquals(3, player.getMediaItemCount());
    }
}

