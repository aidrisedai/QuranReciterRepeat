package com.repeatquran.playback;

import static org.junit.Assert.*;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;

@RunWith(RobolectricTestRunner.class)
public class ResumeCaptureTest {

    @Test
    public void loadRange_capturesResumePrefs() {
        android.content.Context ctx = ApplicationProvider.getApplicationContext();
        ServiceController<PlaybackService> controller = Robolectric.buildService(PlaybackService.class).create().startCommand(0, 0);

        Intent i = new Intent(ctx, PlaybackService.class);
        i.setAction(PlaybackService.ACTION_LOAD_RANGE);
        i.putExtra("ss", 1);
        i.putExtra("sa", 2);
        i.putExtra("es", 1);
        i.putExtra("ea", 3);
        i.putExtra("repeat", 2);
        controller.withIntent(i).startCommand(0, 0);

        android.content.SharedPreferences prefs = ctx.getSharedPreferences("rq_prefs", android.content.Context.MODE_PRIVATE);
        assertEquals("range", prefs.getString("resume.sourceType", null));
        assertEquals(1, prefs.getInt("resume.startSurah", -1));
        assertEquals(2, prefs.getInt("resume.startAyah", -1));
        assertEquals(1, prefs.getInt("resume.endSurah", -1));
        assertEquals(3, prefs.getInt("resume.endAyah", -1));
        assertEquals(2, prefs.getInt("resume.repeat", -1));
    }
}

