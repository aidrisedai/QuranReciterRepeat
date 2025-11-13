package com.repeatquran.recitation;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Base64;
import android.util.Log;
import com.repeatquran.BuildConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Real-time Quran Teacher using Gemini API + Android TTS
 * More reliable than Gemini Live API (which is in early preview)
 * 
 * Flow:
 * 1. Record audio continuously in chunks
 * 2. Send to Gemini for analysis
 * 3. Get feedback as text
 * 4. Speak feedback using Android TTS
 */
public class GeminiRealtimeTeacher {
    private static final String TAG = "GeminiRTTeacher";
    
    // Audio settings
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHUNK_DURATION_MS = 3000; // Analyze every 3 seconds
    
    private final Context context;
    private final Executor executor;
    private AudioRecord audioRecord;
    private boolean isListening = false;
    private SessionCallback callback;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;
    private ComprehensionLevel comprehensionLevel = ComprehensionLevel.MEDIUM;
    private Handler mainHandler;
    private SessionCallback pendingCallback = null;
    
    // Audio buffering
    private File tempAudioFile;
    private FileOutputStream audioOutputStream;
    private int audioChunkCount = 0;
    
    public interface SessionCallback {
        void onSessionStarted();
        void onTeacherSpeaking(String text);
        void onTeacherFinished();
        void onError(String error);
    }
    
    public GeminiRealtimeTeacher(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        // TTS will be initialized on first use
    }
    
    private void initTTS(SessionCallback callback) {
        Log.d(TAG, "Initializing TTS...");
        
        if (tts != null && ttsReady) {
            Log.d(TAG, "TTS already initialized and ready");
            startSessionInternal();
            return;
        }
        
        if (tts != null) {
            Log.d(TAG, "TTS object exists but not ready yet, waiting...");
            pendingCallback = callback;
            return;
        }
        
        pendingCallback = callback;
        
        tts = new TextToSpeech(context, status -> {
            Log.d(TAG, "TTS init callback, status: " + status);
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS SUCCESS, setting language");
                int result = tts.setLanguage(Locale.US);
                Log.d(TAG, "setLanguage result: " + result);
                
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsReady = true;
                    tts.setSpeechRate(0.9f);
                    tts.setPitch(1.0f);
                    Log.d(TAG, "TTS initialized successfully and ready");
                    
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            mainHandler.post(() -> {
                                if (GeminiRealtimeTeacher.this.callback != null) {
                                    GeminiRealtimeTeacher.this.callback.onTeacherSpeaking(utteranceId);
                                }
                            });
                        }
                        
                        @Override
                        public void onDone(String utteranceId) {
                            mainHandler.post(() -> {
                                if (GeminiRealtimeTeacher.this.callback != null) {
                                    GeminiRealtimeTeacher.this.callback.onTeacherFinished();
                                }
                            });
                        }
                        
                        @Override
                        public void onError(String utteranceId) {
                            Log.e(TAG, "TTS error for: " + utteranceId);
                        }
                    });
                    
                    // Now start the session
                    if (pendingCallback != null) {
                        Log.d(TAG, "Starting pending session now that TTS is ready");
                        startSessionInternal();
                        pendingCallback = null;
                    }
                } else {
                    Log.e(TAG, "TTS language not supported: " + result);
                    if (pendingCallback != null) {
                        pendingCallback.onError("Text-to-speech language not available on this device");
                        pendingCallback = null;
                    }
                }
            } else {
                Log.e(TAG, "TTS initialization failed with status: " + status);
                if (pendingCallback != null) {
                    pendingCallback.onError("Text-to-speech initialization failed");
                    pendingCallback = null;
                }
            }
        });
    }
    
    public void setComprehensionLevel(ComprehensionLevel level) {
        this.comprehensionLevel = level;
    }
    
    public void startSession(SessionCallback callback) {
        this.callback = callback;
        Log.d(TAG, "startSession called");
        
        // Initialize TTS if needed, which will automatically start the session when ready
        initTTS(callback);
    }
    
    private void startSessionInternal() {
        executor.execute(() -> {
            try {
                startAudioCapture();
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSessionStarted();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error starting session", e);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
            }
        });
    }
    
    private void startAudioCapture() throws Exception {
        int bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ) * 2;
        
        audioRecord = new AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        );
        
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new Exception("AudioRecord initialization failed");
        }
        
        // Create temp file for audio chunks
        tempAudioFile = new File(context.getCacheDir(), "realtime_audio_" + System.currentTimeMillis() + ".raw");
        audioOutputStream = new FileOutputStream(tempAudioFile);
        
        audioRecord.startRecording();
        isListening = true;
        audioChunkCount = 0;
        
        Log.d(TAG, "Started audio capture");
        
        // Read and analyze audio in chunks
        byte[] buffer = new byte[SAMPLE_RATE * 2 * CHUNK_DURATION_MS / 1000]; // 3 seconds of audio
        
        while (isListening) {
            int read = audioRecord.read(buffer, 0, buffer.length);
            
            if (read > 0) {
                // Write to temp file
                audioOutputStream.write(buffer, 0, read);
                audioOutputStream.flush();
                
                audioChunkCount++;
                
                // Analyze every chunk
                if (audioChunkCount >= 1) {
                    audioChunkCount = 0;
                    
                    // Read the accumulated audio (Android 21+ compatible)
                    byte[] audioData = readFileToByteArray(tempAudioFile);
                    
                    // Send for analysis
                    analyzeAudioChunk(audioData);
                    
                    // Clear temp file
                    audioOutputStream.close();
                    tempAudioFile.delete();
                    tempAudioFile = new File(context.getCacheDir(), "realtime_audio_" + System.currentTimeMillis() + ".raw");
                    audioOutputStream = new FileOutputStream(tempAudioFile);
                }
            }
        }
    }
    
    private byte[] readFileToByteArray(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = fis.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        fis.close();
        return bos.toByteArray();
    }
    
    private void analyzeAudioChunk(byte[] audioData) {
        executor.execute(() -> {
            try {
                String audioBase64 = Base64.encodeToString(audioData, Base64.NO_WRAP);
                
                // Call Gemini API
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key=" + BuildConfig.GEMINI_API_KEY;
                
                JSONObject requestJson = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                
                // Add prompt
                JSONObject textPart = new JSONObject();
                textPart.put("text", ComprehensionPromptBuilder.buildPromptForLevel(comprehensionLevel) + 
                    "\n\nAnalyze this audio clip and provide IMMEDIATE feedback if you hear any mistakes. " +
                    "If the recitation is correct so far, just say 'Continue' or 'Good so far'.");
                parts.put(textPart);
                
                // Add audio
                JSONObject audioPart = new JSONObject();
                JSONObject inlineData = new JSONObject();
                inlineData.put("mimeType", "audio/pcm;rate=16000");
                inlineData.put("data", audioBase64);
                audioPart.put("inlineData", inlineData);
                parts.put(audioPart);
                
                content.put("parts", parts);
                contents.put(content);
                requestJson.put("contents", contents);
                
                // Make HTTP request
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(requestJson.toString());
                wr.flush();
                wr.close();
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    
                    // Parse response
                    JSONObject responseJson = new JSONObject(response.toString());
                    String feedbackText = responseJson
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
                    
                    Log.d(TAG, "Feedback: " + feedbackText);
                    
                    // Speak feedback if it's meaningful (not just "continue")
                    if (!feedbackText.toLowerCase().trim().equals("continue") && 
                        !feedbackText.toLowerCase().trim().equals("good so far") &&
                        feedbackText.length() > 10) {
                        speakFeedback(feedbackText);
                    }
                } else {
                    Log.e(TAG, "API error: " + responseCode);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error analyzing audio chunk", e);
            }
        });
    }
    
    private void speakFeedback(String text) {
        mainHandler.post(() -> {
            if (tts != null && ttsReady) {
                // Stop student's audio while teacher speaks
                if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                
                // Speak the feedback
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "feedback_" + System.currentTimeMillis());
                
                if (callback != null) {
                    callback.onTeacherSpeaking(text);
                }
                
                // Resume recording after speech (with delay)
                mainHandler.postDelayed(() -> {
                    if (isListening && audioRecord != null) {
                        audioRecord.startRecording();
                    }
                }, 1000);
            }
        });
    }
    
    public void stopSession() {
        isListening = false;
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio record", e);
            }
            audioRecord = null;
        }
        
        if (audioOutputStream != null) {
            try {
                audioOutputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing audio stream", e);
            }
        }
        
        if (tempAudioFile != null && tempAudioFile.exists()) {
            tempAudioFile.delete();
        }
        
        if (tts != null) {
            tts.stop();
        }
    }
    
    public boolean isSessionActive() {
        return isListening && audioRecord != null;
    }
    
    public void destroy() {
        stopSession();
        if (tts != null) {
            tts.shutdown();
        }
    }
}
