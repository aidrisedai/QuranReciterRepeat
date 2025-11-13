package com.repeatquran.recitation;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import com.repeatquran.BuildConfig;
import org.json.JSONObject;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class OpenAITTS {
    private static final String TAG = "OpenAITTS";
    private static final String API_URL = "https://api.openai.com/v1/audio/speech";
    
    // Available voices: alloy, echo, fable, onyx, nova, shimmer
    // Onyx: Deep, authoritative male
    // Echo: Clear, professional male
    // Fable: British male accent, expressive storytelling style
    private static final String TEACHER_VOICE = "fable"; // British male, expressive teacher voice
    
    private final Context context;
    private final Executor executor;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    
    public interface TTSCallback {
        void onStart();
        void onComplete();
        void onError(String error);
    }
    
    public OpenAITTS(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Speak text using OpenAI TTS
     * @param text The text to speak (can contain both Arabic and English)
     * @param callback Callback for status updates
     */
    public void speak(String text, TTSCallback callback) {
        executor.execute(() -> {
            try {
                callback.onStart();
                
                Log.d(TAG, "Synthesizing speech with OpenAI TTS");
                
                // Generate audio
                byte[] audioData = synthesize(text);
                if (audioData != null) {
                    playAudio(audioData);
                    callback.onComplete();
                } else {
                    callback.onError("Failed to generate audio");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in speech synthesis", e);
                callback.onError(e.getMessage());
            }
        });
    }
    
    private byte[] synthesize(String text) {
        try {
            if (BuildConfig.OPENAI_API_KEY == null || 
                BuildConfig.OPENAI_API_KEY.isEmpty() ||
                BuildConfig.OPENAI_API_KEY.equals("your_openai_key_here")) {
                Log.w(TAG, "OpenAI API key not configured");
                return null;
            }
            
            // Build request JSON
            JSONObject requestJson = new JSONObject();
            requestJson.put("model", "tts-1"); // Options: tts-1 (faster) or tts-1-hd (higher quality)
            requestJson.put("input", text);
            requestJson.put("voice", TEACHER_VOICE);
            requestJson.put("response_format", "mp3"); // Options: mp3, opus, aac, flac
            requestJson.put("speed", 0.85); // Slower for clearer Arabic pronunciation (0.25-4.0, default 1.0)
            
            // Make HTTP request
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + BuildConfig.OPENAI_API_KEY);
            conn.setDoOutput(true);
            
            // Send request with proper UTF-8 encoding for Arabic text
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            byte[] jsonBytes = requestJson.toString().getBytes("UTF-8");
            wr.write(jsonBytes);
            wr.flush();
            wr.close();
            
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "OpenAI TTS response code: " + responseCode);
            
            if (responseCode == 200) {
                // Read audio stream (returns raw MP3 audio)
                return conn.getInputStream().readAllBytes();
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Log.e(TAG, "OpenAI TTS API error: " + response.toString());
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error synthesizing speech", e);
            return null;
        }
    }
    
    private void playAudio(byte[] audioData) {
        try {
            // Save to temp file and play using MediaPlayer
            File tempFile = File.createTempFile("tts_audio", ".mp3", context.getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(audioData);
            fos.close();
            
            // Play using MediaPlayer
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            isPlaying = true;
            
            // Wait for completion
            while (mediaPlayer.isPlaying()) {
                Thread.sleep(100);
            }
            
            isPlaying = false;
            mediaPlayer.release();
            mediaPlayer = null;
            tempFile.delete();
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio", e);
            isPlaying = false;
        }
    }
    
    public void stop() {
        isPlaying = false;
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
}
