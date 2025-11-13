package com.repeatquran.recitation;

import java.util.ArrayList;
import java.util.List;

public class RecitationFeedback {
    public enum Rating {
        EXCELLENT, GOOD, FAIR, NEEDS_IMPROVEMENT
    }
    
    // Identified verses from the recitation
    public static class VerseIdentification {
        public int surahNumber;
        public String surahName;
        public int startVerse;
        public int endVerse;
        public boolean identified = false;
        
        public String getDisplayText() {
            if (!identified) {
                return "Unable to identify verses clearly";
            }
            if (startVerse == endVerse) {
                return surahName + " (" + surahNumber + ":" + startVerse + ")";
            } else {
                return surahName + " (" + surahNumber + ":" + startVerse + "-" + endVerse + ")";
            }
        }
    }
    
    public VerseIdentification verseIdentification = new VerseIdentification();
    public String arabicText = ""; // Arabic text of the verses for TTS
    public Rating overallRating;
    public String overallComment;
    public String tajweedFeedback;
    public String pronunciationFeedback;
    public String fluencyFeedback;
    public List<String> strengths = new ArrayList<>();
    public List<String> areasForImprovement = new ArrayList<>();
    public String encouragement;
    
    public RecitationFeedback() {
        this.strengths = new java.util.ArrayList<>();
        this.areasForImprovement = new java.util.ArrayList<>();
        this.overallRating = Rating.GOOD;
        this.overallComment = "";
        this.tajweedFeedback = "";
        this.pronunciationFeedback = "";
        this.fluencyFeedback = "";
        this.encouragement = "";
    }
    
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        
        // Rating emoji
        String ratingEmoji = getRatingEmoji();
        sb.append(ratingEmoji).append(" Overall: ").append(overallRating.name().replace('_', ' ')).append("\n\n");
        
        if (overallComment != null && !overallComment.isEmpty()) {
            sb.append("💬 ").append(overallComment).append("\n\n");
        }
        
        if (tajweedFeedback != null && !tajweedFeedback.isEmpty()) {
            sb.append("📖 Tajweed:\n").append(tajweedFeedback).append("\n\n");
        }
        
        if (pronunciationFeedback != null && !pronunciationFeedback.isEmpty()) {
            sb.append("🗣️ Pronunciation:\n").append(pronunciationFeedback).append("\n\n");
        }
        
        if (fluencyFeedback != null && !fluencyFeedback.isEmpty()) {
            sb.append("🎵 Fluency:\n").append(fluencyFeedback).append("\n\n");
        }
        
        if (!strengths.isEmpty()) {
            sb.append("✅ Strengths:\n");
            for (String strength : strengths) {
                sb.append("  • ").append(strength).append("\n");
            }
            sb.append("\n");
        }
        
        if (!areasForImprovement.isEmpty()) {
            sb.append("📈 Areas for Improvement:\n");
            for (String area : areasForImprovement) {
                sb.append("  • ").append(area).append("\n");
            }
            sb.append("\n");
        }
        
        if (encouragement != null && !encouragement.isEmpty()) {
            sb.append("💪 ").append(encouragement);
        }
        
        return sb.toString();
    }
    
    private String getRatingEmoji() {
        switch (overallRating) {
            case EXCELLENT: return "🌟";
            case GOOD: return "👍";
            case FAIR: return "👌";
            case NEEDS_IMPROVEMENT: return "📚";
            default: return "✨";
        }
    }
}
