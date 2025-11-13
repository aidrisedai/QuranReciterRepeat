package com.repeatquran.recitation;

import android.content.Context;
import android.util.Log;
import android.util.Base64;
import com.repeatquran.BuildConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiRecitationAnalyzer {
    private static final String TAG = "GeminiAnalyzer";
    private final Executor executor;
    private final Context context;
    
    public interface AnalysisCallback {
        void onSuccess(RecitationFeedback feedback);
        void onError(String error);
    }
    
    public GeminiRecitationAnalyzer(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public void analyzeRecitation(File audioFile, String verseReference, AnalysisCallback callback) {
        // Check if API key is configured
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.isEmpty()) {
            Log.w(TAG, "Gemini API key not configured, using demo feedback");
            executor.execute(() -> {
                try {
                    Thread.sleep(2000);
                    RecitationFeedback feedback = createDemoFeedback(verseReference);
                    android.os.Handler mainHandler = new android.os.Handler(
                        android.os.Looper.getMainLooper()
                    );
                    mainHandler.post(() -> callback.onSuccess(feedback));
                } catch (Exception e) {
                    android.os.Handler mainHandler = new android.os.Handler(
                        android.os.Looper.getMainLooper()
                    );
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            });
            return;
        }
        
        // Use real Gemini API
        Log.d(TAG, "Analyzing recitation with Gemini AI");
        Log.d(TAG, "Audio file: " + audioFile.getAbsolutePath());
        
        analyzeWithGemini(audioFile, callback);
    }
    
    private RecitationFeedback createDemoFeedback(String verseReference) {
        RecitationFeedback feedback = new RecitationFeedback();
        
        feedback.overallRating = RecitationFeedback.Rating.GOOD;
        feedback.overallComment = "MashaAllah! Your recitation shows good understanding of the basic rules. (Demo mode - add API key for real analysis with verse identification)";
        
        feedback.tajweedFeedback = "Your application of tajweed rules is developing well. Focus on the lengthening (madd) rules for further improvement.";
        
        feedback.pronunciationFeedback = "The pronunciation of Arabic letters is clear. Pay attention to the difference between similar-sounding letters like ح and ه.";
        
        feedback.fluencyFeedback = "Your pace is good for learning. As you memorize better, the flow will become more natural.";
        
        feedback.strengths.add("Clear articulation of letters");
        feedback.strengths.add("Good pace for a learner");
        feedback.strengths.add("Proper stopping points (waqf)");
        
        feedback.areasForImprovement.add("Practice elongation (madd) rules more");
        feedback.areasForImprovement.add("Work on the beauty (tarteel) aspect");
        feedback.areasForImprovement.add("Review rules for noon sakinah");
        
        feedback.encouragement = "Keep practicing regularly! Your dedication to improving your recitation is commendable. Remember, the Prophet ﷺ said: 'The one who recites the Quran and is proficient receives a great reward.' May Allah make it easy for you!";
        
        return feedback;
    }
    
    private void analyzeWithGemini(File audioFile, AnalysisCallback callback) {
        executor.execute(() -> {
            try {
                // Use Gemini REST API with actual audio upload
                Log.d(TAG, "Uploading audio to Gemini API for analysis");
                
                // Read and encode audio file
                byte[] audioBytes = readAudioFile(audioFile);
                String audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP);
                
                // Call Gemini REST API
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key=" + BuildConfig.GEMINI_API_KEY;
                
                // Build JSON request with audio
                JSONObject requestJson = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                
                // Add text prompt
                JSONObject textPart = new JSONObject();
                textPart.put("text", buildAudioAnalysisPrompt());
                parts.put(textPart);
                
                // Add audio data
                JSONObject audioPart = new JSONObject();
                JSONObject inlineData = new JSONObject();
                inlineData.put("mimeType", "audio/mp4");
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
                
                // Send request
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(requestJson.toString());
                wr.flush();
                wr.close();
                
                // Read response
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Gemini API response code: " + responseCode);
                
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(responseCode == 200 ? conn.getInputStream() : conn.getErrorStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                if (responseCode == 200) {
                    // Parse response
                    JSONObject responseJson = new JSONObject(response.toString());
                    String analysisText = responseJson
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
                    
                    Log.d(TAG, "Gemini analysis: " + analysisText);
                    RecitationFeedback feedback = parseFeedback(analysisText);
                    
                    android.os.Handler mainHandler = new android.os.Handler(
                        android.os.Looper.getMainLooper()
                    );
                    mainHandler.post(() -> callback.onSuccess(feedback));
                } else {
                    Log.e(TAG, "Gemini API error: " + response.toString());
                    android.os.Handler mainHandler = new android.os.Handler(
                        android.os.Looper.getMainLooper()
                    );
                    mainHandler.post(() -> callback.onError("API error: " + responseCode));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini API", e);
                android.os.Handler mainHandler = new android.os.Handler(
                    android.os.Looper.getMainLooper()
                );
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    private String buildAudioAnalysisPrompt() {
        return "You are a Quran recitation teacher (Mu'allim). A student has recorded their recitation. Your job:\n\n" +
               "STEP 1: LISTEN carefully to the audio and identify:\n" +
               "- Which Surah and verses they recited\n" +
               "- What they pronounced correctly\n" +
               "- What needs improvement (tajweed mistakes, pronunciation errors, pauses, elongations)\n" +
               "- Any MUTASHABIHAT (similar verses) mistakes - did they mix up similar verses or use wrong wording?\n\n" +
               "STEP 2: PROVIDE FEEDBACK in this EXACT format:\n\n" +
               "SURAH: [Surah name they recited, e.g. 'Al-Fatiha']\n" +
               "SURAH_NUMBER: [Number 1-114]\n" +
               "START_VERSE: [First verse number they recited]\n" +
               "END_VERSE: [Last verse number they recited]\n" +
               "ARABIC_TEXT: [Complete Arabic text of verses they recited]\n" +
               "RATING: [EXCELLENT if nearly perfect | GOOD if 1-2 issues | FAIR if 3-4 issues | NEEDS_IMPROVEMENT if 5+ issues]\n" +
               "TEACHING: [Your teaching feedback - see rules below]\n" +
               "ENCOURAGE: [One warm encouraging sentence]\n\n" +
               "TEACHING RULES (CRITICAL - Format for Text-to-Speech):\n" +
               "Your teaching will be read aloud by TTS. Structure it carefully:\n\n" +
               "1. Use ENGLISH for all explanations and instructions\n" +
               "2. Use PURE ARABIC SCRIPT (with tashkeel) for all Quranic words - TTS will recite them correctly\n" +
               "3. NEVER use transliteration (no \"Qul\", \"As-Samad\", etc.) - only Arabic: قُلْ, الصَّمَد\n\n" +
               "TEACHING FORMAT (must follow exactly):\n" +
               "For each mistake:\n" +
               "   a) EXPLAIN the issue in English: \"Your X letter was too light\"\n" +
               "   b) SAY \"Listen\" or \"Repeat after me\" in English\n" +
               "   c) WRITE the Arabic word/phrase in pure Arabic script\n" +
               "   d) EXPLAIN what to focus on in English\n" +
               "   e) REPEAT the Arabic word again for practice\n\n" +
               "EXAMPLES (showing exact format to follow):\n" +
               "✅ CORRECT: \"Your heavy letter was too light. Listen carefully: الصَّمَدُ - Notice how emphatic that sound is? Now repeat after me: الصَّمَدُ\"\n" +
               "✅ CORRECT: \"The elongation was too short. Listen: اللَّهُ - Hear how I hold that sound for two counts? Practice it: اللَّهُ\"\n" +
               "❌ WRONG: \"Say As-Samad with a heavy Saad\" (no transliteration!)\n" +
               "❌ WRONG: \"الصَّمَدُ should be heavier\" (explain in English first!)\n\n" +
               "Structure for each correction:\n" +
               "1. English explanation → 2. Arabic demonstration → 3. English guidance → 4. Arabic repetition\n\n" +
               "MUTASHABIHAT (SIMILAR VERSES) - CRITICAL:\n" +
               "If they mixed up similar verses or said wrong wording, YOU MUST:\n" +
               "1. Point out the mistake: \"You said X but this verse is Y\"\n" +
               "2. Show what they said WRONG in Arabic\n" +
               "3. Show the CORRECT wording in Arabic\n" +
               "4. Help them remember the difference\n" +
               "Example: \"I noticed you mixed up similar verses. You said: إِنَّا أَنزَلْنَاهُ - But this verse is actually: إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ - Remember, Al-Kawthar starts with 'we gave you' not 'we sent down'.\"\n\n" +
               "Give 2-4 corrections (including mutashabihat if detected) following this pattern.\n\n" +
               "Example full teaching:\n" +
               "\"I heard three areas to work on. First, your throat letter needs more depth. Listen to this: قُلْ - That comes from the back of your throat, not the front. Try it: قُلْ. Second, the emphatic letter was too soft. Listen carefully: الصَّمَدُ - Hear how heavy and pressed that sound is? Practice it slowly: الصَّمَدُ. Third, the elongation here needs to be longer. Listen: كُفُوًا - I held that for two full counts. Now you repeat: كُفُوًا. Excellent work on your pauses!\"\n\n" +
               "Example response:\n" +
               "SURAH: Al-Ikhlas\n" +
               "SURAH_NUMBER: 112\n" +
               "START_VERSE: 1\n" +
               "END_VERSE: 4\n" +
               "ARABIC_TEXT: قُلْ هُوَ اللَّهُ أَحَدٌ اللَّهُ الصَّمَدُ لَمْ يَلِدْ وَلَمْ يُولَدْ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ\n" +
               "RATING: GOOD\n" +
               "TEACHING: I heard two areas to improve. First, your emphatic letter was too soft. Listen carefully: الصَّمَدُ - Hear how heavy that sound is? It comes from pressing your tongue down. Now repeat after me: الصَّمَدُ. Second, the elongation was too short. Listen to this: كُفُوًا - I held that for two full counts. Practice it: كُفُوًا. Your rhythm and pauses were excellent!\n" +
               "ENCOURAGE: You're making steady progress - keep practicing!";
    }
    
    private byte[] readAudioFile(File audioFile) throws IOException {
        FileInputStream fis = new FileInputStream(audioFile);
        byte[] buffer = new byte[(int) audioFile.length()];
        fis.read(buffer);
        fis.close();
        return buffer;
    }
    
    private RecitationFeedback parseFeedback(String analysisText) {
        RecitationFeedback feedback = new RecitationFeedback();
        
        // Parse the structured response
        String[] lines = analysisText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("SURAH:")) {
                feedback.verseIdentification.surahName = line.substring(6).trim();
            } else if (line.startsWith("SURAH_NUMBER:")) {
                try {
                    int num = Integer.parseInt(line.substring(13).trim());
                    if (num > 0 && num <= 114) {
                        feedback.verseIdentification.surahNumber = num;
                        feedback.verseIdentification.identified = true;
                    }
                } catch (NumberFormatException ignored) {}
            } else if (line.startsWith("START_VERSE:")) {
                try {
                    feedback.verseIdentification.startVerse = Integer.parseInt(line.substring(12).trim());
                } catch (NumberFormatException ignored) {}
            } else if (line.startsWith("END_VERSE:")) {
                try {
                    feedback.verseIdentification.endVerse = Integer.parseInt(line.substring(10).trim());
                } catch (NumberFormatException ignored) {}
            } else if (line.startsWith("ARABIC_TEXT:")) {
                String arabicLine = line.substring(12).trim();
                // Only store if it contains actual Arabic characters, not just numbers/references
                if (containsArabic(arabicLine)) {
                    feedback.arabicText = arabicLine;
                }
            } else if (line.startsWith("RATING:")) {
                String rating = line.substring(7).trim();
                feedback.overallRating = parseRating(rating);
            } else if (line.startsWith("TEACHING:")) {
                feedback.overallComment = line.substring(9).trim();
            } else if (line.startsWith("COMMENT:")) {
                feedback.overallComment = line.substring(8).trim();
            } else if (line.startsWith("TAJWEED:")) {
                feedback.tajweedFeedback = line.substring(8).trim();
            } else if (line.startsWith("PRONUNCIATION:")) {
                feedback.pronunciationFeedback = line.substring(14).trim();
            } else if (line.startsWith("FLUENCY:")) {
                feedback.fluencyFeedback = line.substring(8).trim();
            } else if (line.startsWith("STRENGTH:")) {
                feedback.strengths.add(line.substring(9).trim());
            } else if (line.startsWith("IMPROVE:")) {
                feedback.areasForImprovement.add(line.substring(8).trim());
            } else if (line.startsWith("ENCOURAGE:")) {
                feedback.encouragement = line.substring(10).trim();
            }
        }
        
        // Fallbacks if parsing failed
        if (feedback.overallComment == null || feedback.overallComment.isEmpty()) {
            feedback.overallComment = "MashaAllah! Keep practicing your recitation.";
        }
        if (feedback.overallRating == null) {
            feedback.overallRating = RecitationFeedback.Rating.GOOD;
        }
        
        return feedback;
    }
    
    private RecitationFeedback.Rating parseRating(String ratingStr) {
        if (ratingStr.contains("EXCELLENT")) return RecitationFeedback.Rating.EXCELLENT;
        if (ratingStr.contains("GOOD")) return RecitationFeedback.Rating.GOOD;
        if (ratingStr.contains("FAIR")) return RecitationFeedback.Rating.FAIR;
        if (ratingStr.contains("NEEDS_IMPROVEMENT")) return RecitationFeedback.Rating.NEEDS_IMPROVEMENT;
        return RecitationFeedback.Rating.GOOD; // Default
    }
    
    private boolean containsArabic(String text) {
        if (text == null || text.isEmpty()) return false;
        // Check if string contains Arabic Unicode characters (0600-06FF)
        for (char c : text.toCharArray()) {
            if (c >= 0x0600 && c <= 0x06FF) {
                return true;
            }
        }
        return false;
    }
}
