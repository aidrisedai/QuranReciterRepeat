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
}

