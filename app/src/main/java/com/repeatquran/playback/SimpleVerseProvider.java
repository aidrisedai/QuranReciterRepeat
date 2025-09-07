package com.repeatquran.playback;

import java.util.Arrays;
import java.util.List;

/**
 * Simple in-memory provider with placeholder MP3 URLs for demo.
 * Replace with real Quran verse URLs during integration.
 */
public class SimpleVerseProvider implements VerseProvider {
    private final List<String> urls = Arrays.asList(
            // Placeholder public domain sample tracks (replace with real verse URLs)
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
    );

    @Override
    public int size() {
        return urls.size();
    }

    @Override
    public String urlAt(int index) {
        return urls.get(index);
    }
}

