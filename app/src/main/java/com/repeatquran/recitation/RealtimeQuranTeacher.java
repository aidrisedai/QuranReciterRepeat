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
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLSocketFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Real-time Speech-to-Speech Quran Teacher
 * Uses OpenAI Realtime API for live feedback during recitation
 */
public class RealtimeQuranTeacher {
    private static final String TAG = "RealtimeQuranTeacher";
    private static final String REALTIME_API_URL = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-10-01";
    
    // Audio settings
    private static final int SAMPLE_RATE = 24000; // OpenAI Realtime API uses 24kHz
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
    private long sessionStartTime = 0;
    
    public interface SessionCallback {
        void onSessionStarted();
        void onTeacherSpeaking(String text);
        void onTeacherFinished();
        void onError(String error);
    }
    
    public RealtimeQuranTeacher(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    private ComprehensionLevel comprehensionLevel = ComprehensionLevel.MEDIUM;
    private String customInstructions = null;
    
    public void setComprehensionLevel(ComprehensionLevel level) {
        this.comprehensionLevel = level;
    }
    
    public void setCustomInstructions(String instructions) {
        this.customInstructions = instructions;
    }
    
    /**
     * Start a real-time teaching session
     * The teacher will listen and provide live feedback as the student recites
     */
    public void startSession(SessionCallback callback) {
        this.callback = callback;
        
        executor.execute(() -> {
            try {
                connectToRealtimeAPI();
            } catch (Exception e) {
                Log.e(TAG, "Error starting session", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    private void connectToRealtimeAPI() throws Exception {
        // Configure OkHttp for LONG sessions (30-45 minutes)
        OkHttpClient client = new OkHttpClient.Builder()
            .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS) // WebSocket ping every 30s
            .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS) // No read timeout for long sessions
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // 60s write timeout
            .build();
        
        Request request = new Request.Builder()
            .url(REALTIME_API_URL)
            .addHeader("Authorization", "Bearer " + BuildConfig.OPENAI_API_KEY)
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build();
        
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected");
                
                // Initialize session with teacher instructions
                try {
                    JSONObject sessionUpdate = new JSONObject();
                    sessionUpdate.put("type", "session.update");
                    
                    JSONObject session = new JSONObject();
                    session.put("modalities", new org.json.JSONArray().put("text").put("audio"));
                    // Use custom instructions if provided, otherwise use comprehension level
                    String instructions = customInstructions != null ? 
                        customInstructions : 
                        ComprehensionPromptBuilder.buildPromptForLevel(comprehensionLevel, context);
                    session.put("instructions", instructions);
                    // Realtime API voices: alloy, ash, ballad, coral, echo, sage, shimmer, verse, marin, cedar
                    session.put("voice", "ballad"); // Warm, authoritative male - best for teaching
                    session.put("input_audio_format", "pcm16");
                    session.put("output_audio_format", "pcm16");
                    // Configure audio transcription - Realtime API doesn't need explicit model
                    // The API handles transcription internally
                    // NOTE: Transcription failures are non-critical for this use case
                    
                    // Simple 3-second VAD for natural verse-by-verse flow
                    session.put("turn_detection", new JSONObject()
                        .put("type", "server_vad")
                        .put("threshold", 0.5)
                        .put("prefix_padding_ms", 300)
                        .put("silence_duration_ms", 3000)); // 3 seconds - natural pause between verses
                    
                    sessionUpdate.put("session", session);
                    
                    webSocket.send(sessionUpdate.toString());
                    
                    // Track session start time
                    sessionStartTime = System.currentTimeMillis();
                    
                    // Start audio capture and streaming
                    startAudioStreaming();
                    
                    // Start keep-alive monitoring
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
                    String type = message.getString("type");
                    
                    // Log ALL messages for debugging
                    Log.d(TAG, "Received: " + type);
                    if (type.contains("response") || type.contains("audio")) {
                        Log.d(TAG, "Full message: " + text);
                    }
                    
                    switch (type) {
                        case "response.audio_transcript.delta":
                            // Teacher is speaking (text)
                            isTeacherSpeaking = true;
                            if (message.has("delta")) {
                                String delta = message.getString("delta");
                                if (callback != null) {
                                    callback.onTeacherSpeaking(delta);
                                }
                            }
                            break;
                            
                        case "response.audio_transcript.done":
                            // Teacher finished the text part
                            isTeacherSpeaking = false;
                            break;
                            
                        case "response.audio.delta":
                            // Teacher is speaking (audio)
                            if (message.has("delta")) {
                                String audioBase64 = message.getString("delta");
                                playAudio(Base64.decode(audioBase64, Base64.NO_WRAP));
                            }
                            break;
                            
                        case "response.done":
                            // Teacher finished speaking completely
                            isTeacherSpeaking = false;
                            
                            // Check if response failed
                            if (message.has("response")) {
                                JSONObject responseObj = message.getJSONObject("response");
                                String status = responseObj.optString("status", "");
                                
                                if ("failed".equals(status) && responseObj.has("status_details")) {
                                    JSONObject statusDetails = responseObj.getJSONObject("status_details");
                                    if (statusDetails.has("error")) {
                                        JSONObject error = statusDetails.getJSONObject("error");
                                        String errorType = error.optString("type", "unknown");
                                        String errorMsg = error.optString("message", "Unknown error");
                                        
                                        Log.e(TAG, "Response failed: " + errorType + " - " + errorMsg);
                                        
                                        // Show user-friendly error message
                                        if (callback != null) {
                                            if ("insufficient_quota".equals(errorType)) {
                                                callback.onError("OpenAI API quota exceeded. Please check your billing at platform.openai.com/account/billing");
                                            } else {
                                                callback.onError("API Error: " + errorMsg);
                                            }
                                        }
                                        return; // Don't call onTeacherFinished for failed responses
                                    }
                                }
                            }
                            
                            if (callback != null) {
                                callback.onTeacherFinished();
                            }
                            break;
                            
                        case "conversation.item.input_audio_transcription.failed":
                            // Transcription failed - this is non-critical for our use case
                            // The AI teacher can still respond based on the audio itself
                            Log.w(TAG, "Audio transcription failed (non-critical - AI can still hear audio)");
                            if (message.has("error")) {
                                JSONObject errorObj = message.getJSONObject("error");
                                String errorMsg = errorObj.optString("message", "Unknown");
                                String errorCode = errorObj.optString("code", "Unknown");
                                Log.d(TAG, "Transcription error: " + errorCode + " - " + errorMsg);
                            }
                            // Don't notify callback - this is expected for some audio
                            break;
                            
                        case "conversation.item.input_audio_transcription.completed":
                            // Transcription succeeded - log for debugging
                            if (message.has("transcript")) {
                                String transcript = message.getString("transcript");
                                Log.d(TAG, "Student said: " + transcript);
                            }
                            break;
                            
                        case "response.created":
                            // Response started - log details
                            if (message.has("response")) {
                                JSONObject responseObj = message.getJSONObject("response");
                                Log.d(TAG, "Response created with status: " + responseObj.optString("status", "unknown"));
                            }
                            break;
                            
                        case "response.output_item.added":
                            // Output item being added to response
                            Log.d(TAG, "Output item added: " + message.toString());
                            break;
                            
                        case "response.content_part.added":
                            // Content part added
                            Log.d(TAG, "Content part added: " + message.toString());
                            break;
                            
                        case "response.audio.done":
                            // All audio received for this response
                            Log.d(TAG, "Audio output complete");
                            break;
                            
                        case "response.output_item.done":
                            // Output item completed
                            Log.d(TAG, "Output item done");
                            break;
                            
                        case "error":
                            String error = message.has("error") ? 
                                message.getJSONObject("error").getString("message") : "Unknown error";
                            Log.e(TAG, "API Error: " + error);
                            if (callback != null) {
                                callback.onError(error);
                            }
                            break;
                            
                        default:
                            // Log any unhandled message types
                            if (type.startsWith("response.") || type.startsWith("conversation.")) {
                                Log.d(TAG, "Unhandled message type: " + type + " - " + message.toString());
                            }
                            break;
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error handling message", e);
                }
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket error: " + t.getMessage(), t);
                
                // Check if it's a broken pipe or timeout error
                String errorMsg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
                boolean isBrokenPipe = errorMsg.contains("broken pipe") || 
                                      errorMsg.contains("timeout") ||
                                      errorMsg.contains("connection reset");
                
                if (isBrokenPipe && isListening) {
                    Log.w(TAG, "Connection lost - attempting to reconnect...");
                    
                    // Attempt to reconnect once
                    executor.execute(() -> {
                        try {
                            Thread.sleep(1000); // Wait a second
                            if (isListening && callback != null) {
                                // Notify user
                                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                                mainHandler.post(() -> {
                                    if (callback != null) {
                                        callback.onError("Connection lost. Please restart the session.");
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Reconnect failed", e);
                        }
                    });
                } else {
                    if (callback != null) {
                        callback.onError(t.getMessage());
                    }
                }
            }
        });
    }
    
    private String buildTeacherInstructions() {
        return "You are an experienced Quran recitation teacher (Mu'allim) listening to a student recite in real-time.\n\n" +
               "CRITICAL RULES:\n" +
               "1. LISTEN MORE, INTERRUPT LESS\n" +
               "   - Let them recite at least 2-3 verses before stopping\n" +
               "   - Only interrupt for SIGNIFICANT mistakes (not tiny imperfections)\n" +
               "   - If they're doing well, just say \"Good, continue\" and let them go\n\n" +
               "2. TRACK WHICH VERSES THEY'RE RECITING\n" +
               "   - Pay attention to which Surah and verses they're reading\n" +
               "   - If they jump to a different Surah or verse unexpectedly, gently guide them back:\n" +
               "     \"Wait, you were reading Surah Al-Ikhlas. Did you mean to switch to Al-Nas? Let's continue with Al-Ikhlas from where we were.\"\n" +
               "   - If they mix up similar verses (mutashabihat), correct them immediately:\n" +
               "     \"Hold on - you're mixing verses. This is Al-Ikhlas which says: قُلْ هُوَ اللَّهُ أَحَدٌ - not Al-Kawthar. Let's go back.\"\n\n" +
               "3. WHEN YOU CORRECT, READ THE ARABIC ALOUD\n" +
               "   - ALWAYS speak the Arabic words clearly so they can hear correct pronunciation\n" +
               "   - Example: \"That letter needs more emphasis. Listen: الصَّمَدُ [pause] Hear how heavy that is? Now you try: الصَّمَدُ\"\n" +
               "   - Don't just describe in English - RECITE the correction in Arabic\n\n" +
               "4. KNOW WHEN TO INTERRUPT:\n" +
               "   Interrupt for:\n" +
               "   - Wrong verse/Surah (mutashabihat confusion)\n" +
               "   - Heavy letters pronounced too lightly\n" +
               "   - Skipping or adding words\n" +
               "   - Major tajweed violations\n" +
               "   \n" +
               "   DO NOT interrupt for:\n" +
               "   - Minor imperfections\n" +
               "   - Slight rhythm variations\n" +
               "   - If they're clearly learning/practicing\n\n" +
               "5. CORRECTION FORMAT:\n" +
               "   Step 1 - Stop gently: \"Wait a moment\"\n" +
               "   Step 2 - Explain briefly: \"That heavy letter was too soft\"\n" +
               "   Step 3 - READ the Arabic correctly (SPEAK IT): الصَّمَدُ\n" +
               "   Step 4 - Explain what to notice: \"Hear how I pressed that sound?\"\n" +
               "   Step 5 - Have them repeat: \"Try it: الصَّمَدُ\"\n" +
               "   Step 6 - Resume clearly: \"Good! Continue from: اللَّهُ الصَّمَدُ\"\n\n" +
               "6. MUTASHABIHAT (VERSE CONFUSION) HANDLING:\n" +
               "   If they recite wrong verses or mix them up:\n" +
               "   - \"Hold on - I think you're confusing verses. You started with [Surah name] but now you're saying words from [different Surah].\"\n" +
               "   - Recite the CORRECT verse in Arabic: [Arabic text]\n" +
               "   - \"This is the verse you should be reading. Let's start this verse again from the beginning: [Arabic text]\"\n\n" +
               "EXAMPLES OF GOOD TEACHING:\n" +
               "\n" +
               "Example 1 - Minor issue, let it go:\n" +
               "Student: [recites 3 verses with slight imperfection]\n" +
               "You: \"MashaAllah, good pace. Continue.\"\n" +
               "\n" +
               "Example 2 - Heavy letter correction:\n" +
               "Student: [says الصمد with light ص]\n" +
               "You: \"Wait - that ص needs to be heavier. Listen to how I say it: الصَّمَدُ [speak it clearly]. Hear how it comes from deep in the throat? Try it: الصَّمَدُ. Good! Now continue from: اللَّهُ الصَّمَدُ\"\n" +
               "\n" +
               "Example 3 - Wrong verse/mutashabihat:\n" +
               "Student: [starts Al-Ikhlas but says words from Al-Kawthar]\n" +
               "You: \"Wait wait - you're mixing up similar verses. This is Al-Ikhlas, not Al-Kawthar. Al-Ikhlas says: قُلْ هُوَ اللَّهُ أَحَدٌ [recite it]. Let's start Al-Ikhlas again from the beginning.\"\n\n" +
               "REMEMBER:\n" +
               "- Be patient - let them recite more before interrupting\n" +
               "- Track which verses they're reading\n" +
               "- When correcting, ALWAYS speak the Arabic aloud\n" +
               "- Help them stay on track if they switch verses\n" +
               "- Be warm but clear in your corrections\n" +
               "- Only stop for meaningful corrections, not every tiny thing\n\n" +
               "You are a skilled teacher who knows when to let the student flow and when to correct.";
    }
    
    private void startAudioStreaming() {
        executor.execute(() -> {
            try {
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize * 2); // Double buffer for stability
                
                audioRecord.startRecording();
                isListening = true;
                
                // Use smaller chunks for streaming (send every 100ms instead of full buffer)
                byte[] buffer = new byte[SAMPLE_RATE * 2 / 10]; // 100ms chunks (24000Hz * 2 bytes * 0.1s)
                
                while (isListening) {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        // Only send audio when teacher is NOT speaking
                        if (!isTeacherSpeaking) {
                            try {
                                JSONObject audioEvent = new JSONObject();
                                audioEvent.put("type", "input_audio_buffer.append");
                                audioEvent.put("audio", Base64.encodeToString(buffer, 0, bytesRead, Base64.NO_WRAP));
                                
                                if (webSocket != null) {
                                    webSocket.send(audioEvent.toString());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error sending audio", e);
                            }
                        }
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error streaming audio", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    private void playAudio(byte[] audioData) {
        try {
            if (audioTrack == null) {
                int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, 
                    AudioFormat.CHANNEL_OUT_MONO, AUDIO_FORMAT);
                
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
        keepAliveHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                // For 30-45 minute sessions, OkHttp's built-in ping is sufficient
                // No need to send manual events - just check connection is alive
                if (webSocket != null && isListening) {
                    Log.d(TAG, "Session alive - running for " + 
                        ((System.currentTimeMillis() - sessionStartTime) / 1000) + " seconds");
                    
                    // Schedule next check in 30 seconds
                    keepAliveHandler.postDelayed(this, 30000);
                }
            }
        };
        
        // Start monitoring after 30 seconds
        keepAliveHandler.postDelayed(keepAliveRunnable, 30000);
    }
    
    public void stopSession() {
        isListening = false;
        
        // Stop keep-alive
        if (keepAliveHandler != null && keepAliveRunnable != null) {
            keepAliveHandler.removeCallbacks(keepAliveRunnable);
        }
        
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
        
        if (webSocket != null) {
            webSocket.close(1000, "Session ended");
            webSocket = null;
        }
    }
}
