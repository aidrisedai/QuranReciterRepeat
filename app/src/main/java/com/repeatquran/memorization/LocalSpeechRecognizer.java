package com.repeatquran.memorization;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.ArrayList;

/**
 * Wrapper for Android SpeechRecognizer with continuous Arabic recognition
 */
public class LocalSpeechRecognizer {
    private static final String TAG = "LocalSpeechRecognizer";
    private static final String LANGUAGE = "ar"; // Arabic
    
    private Context context;
    private SpeechRecognizer speechRecognizer;
    private TranscriptionCallback callback;
    private boolean isListening = false;
    private StringBuilder continuousTranscript = new StringBuilder();
    private int restartCount = 0;
    private static final int MAX_RESTARTS = 3;
    
    public interface TranscriptionCallback {
        void onPartialResult(String text);
        void onFinalResult(String text);
        void onError(String error);
    }
    
    public LocalSpeechRecognizer(Context context) {
        this.context = context;
    }
    
    /**
     * Check if speech recognition is available on this device
     */
    public static boolean isAvailable(Context context) {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }
    
    /**
     * Start continuous listening
     */
    public void startListening(TranscriptionCallback callback) {
        this.callback = callback;
        this.isListening = true;
        this.restartCount = 0;
        
        if (!isAvailable(context)) {
            if (callback != null) {
                callback.onError("Speech recognition not available on this device");
            }
            return;
        }
        
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(recognitionListener);
        }
        
        Log.d(TAG, "Starting speech recognition");
        startRecognition();
    }
    
    /**
     * Stop listening
     */
    public void stopListening() {
        Log.d(TAG, "Stopping speech recognition");
        isListening = false;
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        Log.d(TAG, "Destroying speech recognizer");
        isListening = false;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
    
    /**
     * Get the full continuous transcript
     */
    public String getContinuousTranscript() {
        return continuousTranscript.toString().trim();
    }
    
    /**
     * Clear the transcript
     */
    public void clearTranscript() {
        continuousTranscript.setLength(0);
    }
    
    private void startRecognition() {
        if (!isListening) {
            return;
        }
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        
        try {
            speechRecognizer.startListening(intent);
            restartCount = 0; // Reset on successful start
        } catch (Exception e) {
            Log.e(TAG, "Error starting recognition", e);
            if (callback != null) {
                callback.onError("Failed to start recognition: " + e.getMessage());
            }
        }
    }
    
    private RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "Ready for speech");
        }
        
        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "Speech started");
        }
        
        @Override
        public void onRmsChanged(float rmsdB) {
            // Audio level changed - could use for visual feedback
        }
        
        @Override
        public void onBufferReceived(byte[] buffer) {
            // Audio buffer received
        }
        
        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "Speech ended");
        }
        
        @Override
        public void onError(int error) {
            String errorMessage = getErrorMessage(error);
            Log.e(TAG, "Speech recognition error: " + errorMessage + " (code: " + error + ")");
            
            // Handle different error types
            boolean shouldRestart = true;
            
            switch (error) {
                case SpeechRecognizer.ERROR_CLIENT:
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    shouldRestart = false;
                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                    break;
                    
                case SpeechRecognizer.ERROR_NETWORK:
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    if (callback != null) {
                        callback.onError("Network error - check internet connection");
                    }
                    break;
                    
                case SpeechRecognizer.ERROR_NO_MATCH:
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    // Common errors - just restart
                    Log.d(TAG, "No speech detected, restarting...");
                    break;
            }
            
            // Restart for continuous listening (with limit)
            if (isListening && shouldRestart) {
                if (restartCount < MAX_RESTARTS) {
                    restartCount++;
                    Log.d(TAG, "Restarting recognition (attempt " + restartCount + ")");
                    startRecognition();
                } else {
                    Log.w(TAG, "Max restart attempts reached");
                    if (callback != null) {
                        callback.onError("Speech recognition repeatedly failed");
                    }
                }
            }
        }
        
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION);
            
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                Log.d(TAG, "Final result: " + text);
                
                // Append to continuous transcript
                if (continuousTranscript.length() > 0) {
                    continuousTranscript.append(" ");
                }
                continuousTranscript.append(text);
                
                if (callback != null) {
                    callback.onFinalResult(text);
                }
            }
            
            // Restart for continuous listening
            if (isListening) {
                Log.d(TAG, "Restarting for continuous recognition");
                startRecognition();
            }
        }
        
        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION);
            
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                Log.d(TAG, "Partial result: " + text);
                
                if (callback != null) {
                    callback.onPartialResult(text);
                }
            }
        }
        
        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "Event: " + eventType);
        }
    };
    
    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognizer busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Speech timeout";
            default:
                return "Unknown error: " + error;
        }
    }
}
