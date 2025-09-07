package com.repeatquran.playback;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlaybackViewModelTest {

    @Test
    public void instantiatesAndDefaultsNotPlaying() {
        PlaybackViewModel vm = new PlaybackViewModel();
        assertNotNull(vm);
        assertFalse(vm.isPlaying());
    }
}

