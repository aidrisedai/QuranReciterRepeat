package com.repeatquran.recitation;

/**
 * Comprehension levels for Quran recitation feedback
 * Determines how detailed and strict the teacher's corrections will be
 */
public enum ComprehensionLevel {
    SIMPLE("Simple", "Basic corrections - 1-2 main mistakes", 
           "For beginners learning basic recitation"),
    
    MEDIUM("Medium", "Detailed feedback with improvement tips",
           "For intermediate learners improving tajweed"),
    
    HIGH("High", "Expert-level corrections - every detail matters",
         "For advanced learners mastering qira'ah. Checks: makharijul huruf, shifatul huruf, tajwid, waqf, ibtida'");
    
    private final String displayName;
    private final String shortDescription;
    private final String fullDescription;
    
    ComprehensionLevel(String displayName, String shortDescription, String fullDescription) {
        this.displayName = displayName;
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getShortDescription() {
        return shortDescription;
    }
    
    public String getFullDescription() {
        return fullDescription;
    }
    
    /**
     * Get icon emoji for this level
     */
    public String getIcon() {
        switch (this) {
            case SIMPLE: return "🌱";
            case MEDIUM: return "🌿";
            case HIGH: return "🌳";
            default: return "";
        }
    }
}
