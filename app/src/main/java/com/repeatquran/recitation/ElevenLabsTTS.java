package com.repeatquran.recitation;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Base64;
import android.util.Log;
import com.repeatquran.BuildConfig;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ElevenLabsTTS {
    private static final String TAG = "ElevenLabsTTS";
    private static final String API_BASE = "https://api.elevenlabs.io/v1";
    
    // Multilingual voices that can speak both Arabic and English
    // Using voices with Arabic accent that handle both languages naturally
    private static final String TEACHER_VOICE_ID = "onwK4e9ZLuTAKqWW03F9"; // Daniel - British accent, good for teaching
    private static final String ARABIC_TEACHER_VOICE_ID = "EXAVITQu4vr4xnSDxMaL"; // Sarah - multilingual, warm voice
    
    private final Context context;
    private final Executor executor;
    private AudioTrack audioTrack;
    private boolean isPlaying = false;
    
    public interface TTSCallback {
        void onStart();
        void onComplete();
        void onError(String error);
    }
    
    public ElevenLabsTTS(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public void speakBilingual(String arabicText, String englishText, TTSCallback callback) {
        executor.execute(() -> {
            try {
                callback.onStart();
                
                // Combine Arabic and English into one natural teaching session
                // The multilingual model will handle both languages seamlessly
                String combinedText = arabicText;
                if (!englishText.isEmpty()) {
                    combinedText += " ... " + englishText;
                }
                
                Log.d(TAG, "Speaking bilingual text: " + combinedText.substring(0, Math.min(100, combinedText.length())));
                
                // Use multilingual voice that can handle both Arabic and English
                byte[] audio = synthesize(combinedText, ARABIC_TEACHER_VOICE_ID);
                if (audio != null) {
                    playAudio(audio);
                    callback.onComplete();
                } else {
                    callback.onError("Failed to generate audio");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in bilingual speech", e);
                callback.onError(e.getMessage());
            }
        });
    }
    
    private void speak(String text, String voiceId, TTSCallback callback) {
        executor.execute(() -> {
            try {
                callback.onStart();
                
                byte[] audioData = synthesize(text, voiceId);
                if (audioData != null) {
                    playAudio(audioData);
                    callback.onComplete();
                } else {
                    callback.onError("Failed to synthesize speech");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error speaking", e);
                callback.onError(e.getMessage());
            }
        });
    }
    
    private byte[] synthesize(String text, String voiceId) {
        try {
            if (BuildConfig.ELEVENLABS_API_KEY == null || 
                BuildConfig.ELEVENLABS_API_KEY.isEmpty() ||
                BuildConfig.ELEVENLABS_API_KEY.equals("your_elevenlabs_key_here")) {
                Log.w(TAG, "ElevenLabs API key not configured");
                return null;
            }
            
            String apiUrl = API_BASE + "/text-to-speech/" + voiceId + "/stream";
            
            // Build request JSON
            JSONObject requestJson = new JSONObject();
            requestJson.put("text", text);
            requestJson.put("model_id", "eleven_multilingual_v2"); // Best quality for emotional teaching with Arabic + English
            
            JSONObject voiceSettings = new JSONObject();
            voiceSettings.put("stability", 0.5);
            voiceSettings.put("similarity_boost", 0.75);
            voiceSettings.put("style", 0.0);
            voiceSettings.put("use_speaker_boost", true);
            requestJson.put("voice_settings", voiceSettings);
            
            // Make HTTP request
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("xi-api-key", BuildConfig.ELEVENLABS_API_KEY);
            conn.setDoOutput(true);
            
            // Send request with proper UTF-8 encoding for Arabic text
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            byte[] jsonBytes = requestJson.toString().getBytes("UTF-8");
            wr.write(jsonBytes);
            wr.flush();
            wr.close();
            
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "ElevenLabs response code: " + responseCode);
            
            if (responseCode == 200) {
                // Read audio stream
                return conn.getInputStream().readAllBytes();
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Log.e(TAG, "ElevenLabs API error: " + response.toString());
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error synthesizing speech", e);
            return null;
        }
    }
    
    private void playAudio(byte[] audioData) {
        try {
            // MP3 audio from ElevenLabs - use MediaPlayer instead
            // For now, save to temp file and play
            java.io.File tempFile = java.io.File.createTempFile("tts_audio", ".mp3", context.getCacheDir());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            fos.write(audioData);
            fos.close();
            
            // Play using MediaPlayer
            android.media.MediaPlayer mp = new android.media.MediaPlayer();
            mp.setDataSource(tempFile.getAbsolutePath());
            mp.prepare();
            mp.start();
            
            // Wait for completion
            while (mp.isPlaying()) {
                Thread.sleep(100);
            }
            
            mp.release();
            tempFile.delete();
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio", e);
        }
    }
    
    public void stop() {
        isPlaying = false;
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
}
