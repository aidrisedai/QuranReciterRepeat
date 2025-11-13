package com.repeatquran.data;

import android.content.Context;

import com.repeatquran.data.db.QuizResultDao;
import com.repeatquran.data.db.QuizResultEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;

import java.util.List;

public class QuizResultRepository {
    private final QuizResultDao dao;
    
    public QuizResultRepository(Context context) {
        this.dao = RepeatQuranDatabase.get(context).quizResultDao();
    }
    
    public long insert(QuizResultEntity result) {
        return dao.insert(result);
    }
    
    public List<QuizResultEntity> getBySession(long sessionId) {
        return dao.getBySession(sessionId);
    }
    
    public List<QuizResultEntity> getIncorrectBySession(long sessionId) {
        return dao.getIncorrectBySession(sessionId);
    }
    
    public List<QuizResultEntity> getRecentForVerse(int surah, int ayah) {
        return dao.getRecentForVerse(surah, ayah);
    }
    
    public int getCorrectCount(long sessionId) {
        return dao.getCorrectCount(sessionId);
    }
    
    public int getTotalCount(long sessionId) {
        return dao.getTotalCount(sessionId);
    }
    
    public List<QuizResultEntity> getMostProblematicVerses(int limit) {
        return dao.getMostProblematicVerses(limit);
    }
}
