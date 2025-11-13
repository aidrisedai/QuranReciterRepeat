package com.repeatquran.recitation;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import com.repeatquran.BuildConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Real-time Speech-to-Speech Quran Teacher using Gemini 2.0 Live API
 * Provides live feedback during recitation with natural voice output
 */
public class GeminiLiveQuranTeacher {
    private static final String TAG = "GeminiLiveTeacher";
    
    // Gemini Live API endpoint - trying different formats
    // Format 1: Full service path
    private static final String GEMINI_LIVE_API_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent";
    // Format 2: Shorter path (alternative)
    // private static final String GEMINI_LIVE_API_URL = "wss://generativelanguage.googleapis.com/ws/v1alpha/models/gemini-2.0-flash-exp:streamGenerateContent";
    
    // Audio settings - Gemini requires 16kHz, 16-bit PCM, mono
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private final Context context;
    private final Executor executor;
    private WebSocket webSocket;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private boolean isListening = false;
    private boolean isTeacherSpeaking = false;
    private SessionCallback callback;
    private Handler keepAliveHandler;
    private Runnable keepAliveRunnable;
    private ComprehensionLevel comprehensionLevel = ComprehensionLevel.MEDIUM;
    
    public interface SessionCallback {
        void onSessionStarted();
        void onTeacherSpeaking(String text);
        void onTeacherFinished();
        void onError(String error);
    }
    
    public GeminiLiveQuranTeacher(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.keepAliveHandler = new Handler(Looper.getMainLooper());
    }
    
    public void setComprehensionLevel(ComprehensionLevel level) {
        this.comprehensionLevel = level;
    }
    
    /**
     * Start a real-time teaching session
     */
    public void startSession(SessionCallback callback) {
        this.callback = callback;
        
        executor.execute(() -> {
            try {
                connectToGeminiLive();
            } catch (Exception e) {
                Log.e(TAG, "Error starting session", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    private void connectToGeminiLive() throws Exception {
        // Check if API key is available
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.isEmpty()) {
            throw new Exception("Gemini API key not configured. Add GEMINI_API_KEY to local.properties");
        }
        
        // Configure OkHttp with longer timeouts
        OkHttpClient client = new OkHttpClient.Builder()
            .pingInterval(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        
        // Build WebSocket URL with API key
        String wsUrl = GEMINI_LIVE_API_URL + "?key=" + BuildConfig.GEMINI_API_KEY;
        Log.d(TAG, "Connecting to: " + GEMINI_LIVE_API_URL);
        
        Request request = new Request.Builder()
            .url(wsUrl)
            .build();
        
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected to Gemini Live");
                
                try {
                    // Initialize session configuration
                    JSONObject setup = new JSONObject();
                    setup.put("setup", new JSONObject()
                        .put("model", "models/gemini-2.0-flash-exp")
                        .put("generation_config", new JSONObject()
                            .put("response_modalities", new JSONArray().put("AUDIO"))
                            .put("speech_config", new JSONObject()
                                .put("voice_config", new JSONObject()
                                    .put("prebuilt_voice_config", new JSONObject()
                                        .put("voice_name", "Aoede")))))  // Warm, authoritative voice
                        .put("system_instruction", new JSONObject()
                            .put("parts", new JSONArray()
                                .put(new JSONObject()
                                    .put("text", ComprehensionPromptBuilder.buildPromptForLevel(comprehensionLevel))))));
                    
                    webSocket.send(setup.toString());
                    Log.d(TAG, "Sent setup: " + setup.toString());
                    
                    // Start audio streaming
                    startAudioStreaming();
                    
                    // Start keep-alive
                    startKeepAlive();
                    
                    if (callback != null) {
                        callback.onSessionStarted();
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing session", e);
                }
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject message = new JSONObject(text);
                    Log.d(TAG, "Received: " + message.toString());
                    
                    // Handle server content (audio response)
                    if (message.has("serverContent")) {
                        JSONObject serverContent = message.getJSONObject("serverContent");
                        
                        if (serverContent.has("modelTurn")) {
                            JSONObject modelTurn = serverContent.getJSONObject("modelTurn");
                            JSONArray parts = modelTurn.getJSONArray("parts");
                            
                            for (int i = 0; i < parts.length(); i++) {
                                JSONObject part = parts.getJSONObject(i);
                                
                                // Handle inline audio data
                                if (part.has("inlineData")) {
                                    JSONObject inlineData = part.getJSONObject("inlineData");
                                    String mimeType = inlineData.getString("mimeType");
                                    String audioBase64 = inlineData.getString("data");
                                    
                                    if (mimeType.contains("audio/pcm")) {
                                        isTeacherSpeaking = true;
                                        byte[] audioData = Base64.decode(audioBase64, Base64.NO_WRAP);
                                        playAudio(audioData);
                                    }
                                }
                                
                                // Handle text (for display)
                                if (part.has("text")) {
                                    String text_part = part.getString("text");
                                    if (callback != null) {
                                        callback.onTeacherSpeaking(text_part);
                                    }
                                }
                            }
                        }
                        
                        // Check if turn is complete
                        if (serverContent.has("turnComplete") && serverContent.getBoolean("turnComplete")) {
                            isTeacherSpeaking = false;
                            if (callback != null) {
                                callback.onTeacherFinished();
                            }
                        }
                    }
                    
                    // Handle setup complete
                    if (message.has("setupComplete")) {
                        Log.d(TAG, "Setup complete");
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error handling message", e);
                }
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket error", t);
                if (response != null) {
                    Log.e(TAG, "Response code: " + response.code());
                    Log.e(TAG, "Response message: " + response.message());
                }
                if (callback != null) {
                    String errorMsg = t.getMessage();
                    boolean isBrokenPipe = errorMsg != null && 
                        (errorMsg.contains("broken pipe") || 
                         errorMsg.contains("timeout") ||
                         errorMsg.contains("connection reset"));
                    
                    if (isBrokenPipe) {
                        callback.onError("Connection lost. Please try again with a shorter recitation.");
                    } else {
                        callback.onError("Connection error: " + errorMsg);
                    }
                }
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                stopSession();
            }
        });
    }
    
    private void startAudioStreaming() {
        executor.execute(() -> {
            try {
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
                    Log.e(TAG, "AudioRecord initialization failed");
                    return;
                }
                
                audioRecord.startRecording();
                isListening = true;
                
                // Stream audio in chunks (100ms chunks for responsiveness)
                byte[] buffer = new byte[SAMPLE_RATE * 2 / 10];
                
                while (isListening && webSocket != null) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    
                    if (read > 0 && !isTeacherSpeaking) {
                        try {
                            // Send audio chunk to Gemini
                            String audioBase64 = Base64.encodeToString(buffer, 0, read, Base64.NO_WRAP);
                            
                            JSONObject realtimeInput = new JSONObject();
                            realtimeInput.put("realtimeInput", new JSONObject()
                                .put("mediaChunks", new JSONArray()
                                    .put(new JSONObject()
                                        .put("mimeType", "audio/pcm;rate=16000")
                                        .put("data", audioBase64))));
                            
                            webSocket.send(realtimeInput.toString());
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error sending audio", e);
                        }
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Audio streaming error", e);
            }
        });
    }
    
    private void playAudio(byte[] audioData) {
        try {
            if (audioTrack == null) {
                int bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AUDIO_FORMAT
                );
                
                audioTrack = new AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AUDIO_FORMAT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                );
                
                audioTrack.play();
            }
            
            audioTrack.write(audioData, 0, audioData.length);
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio", e);
        }
    }
    
    private void startKeepAlive() {
        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                if (webSocket != null && isListening) {
                    try {
                        // Send a ping or empty message to keep connection alive
                        webSocket.send("{}");
                    } catch (Exception e) {
                        Log.e(TAG, "Keep-alive error", e);
                    }
                    keepAliveHandler.postDelayed(this, 15000); // Every 15 seconds
                }
            }
        };
        keepAliveHandler.postDelayed(keepAliveRunnable, 15000);
    }
    
    public void stopSession() {
        isListening = false;
        isTeacherSpeaking = false;
        
        if (keepAliveHandler != null && keepAliveRunnable != null) {
            keepAliveHandler.removeCallbacks(keepAliveRunnable);
        }
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio record", e);
            }
            audioRecord = null;
        }
        
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio track", e);
            }
            audioTrack = null;
        }
        
        if (webSocket != null) {
            webSocket.close(1000, "Session ended");
            webSocket = null;
        }
    }
    
    public boolean isSessionActive() {
        return isListening && webSocket != null;
    }
}
