package com.repeatquran.data;

import android.content.Context;

import com.repeatquran.data.db.RepeatQuranDatabase;
import com.repeatquran.data.db.SessionDao;
import com.repeatquran.data.db.SessionEntity;

public class SessionRepository {
    private final SessionDao dao;

    public SessionRepository(Context context) {
        this.dao = RepeatQuranDatabase.get(context).sessionDao();
    }

    public long insert(SessionEntity e) {
        return dao.insert(e);
    }

    public void markEnded(long id, long endedAt, Integer cyclesCompleted) {
        dao.markEnded(id, endedAt, cyclesCompleted);
    }

    public java.util.List<SessionEntity> getLastSessions(int n) {
        return dao.getLastN(n);
    }
    
    // NEW: Session type specific methods
    public java.util.List<SessionEntity> getSessionsByType(String type, int limit) {
        return dao.getByType(type, limit);
    }
    
    public java.util.List<SessionEntity> getSessionsByGoal(long goalId) {
        return dao.getByGoal(goalId);
    }
    
    public java.util.List<SessionEntity> getSessionsByTypeSince(String type, long startTime) {
        return dao.getByTypeSince(type, startTime);
    }
    
    public int countSessionsByTypeSince(String type, long startTime) {
        return dao.countByTypeSince(type, startTime);
    }
}
