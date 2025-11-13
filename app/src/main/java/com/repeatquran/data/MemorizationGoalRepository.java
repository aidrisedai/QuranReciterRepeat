package com.repeatquran.data;

import android.content.Context;

import com.repeatquran.data.db.MemorizationGoalDao;
import com.repeatquran.data.db.MemorizationGoalEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;

import java.util.List;

public class MemorizationGoalRepository {
    private final MemorizationGoalDao dao;
    
    public MemorizationGoalRepository(Context context) {
        this.dao = RepeatQuranDatabase.get(context).memorizationGoalDao();
    }
    
    public long insert(MemorizationGoalEntity goal) {
        return dao.insert(goal);
    }
    
    public void update(MemorizationGoalEntity goal) {
        dao.update(goal);
    }
    
    public MemorizationGoalEntity getById(long id) {
        return dao.getById(id);
    }
    
    public MemorizationGoalEntity getActiveGoal() {
        return dao.getActiveGoal();
    }
    
    public List<MemorizationGoalEntity> getAllActive() {
        return dao.getAllActive();
    }
    
    public List<MemorizationGoalEntity> getCompleted() {
        return dao.getCompleted();
    }
    
    public List<MemorizationGoalEntity> getPaused() {
        return dao.getPaused();
    }
    
    public List<MemorizationGoalEntity> getAll() {
        return dao.getAll();
    }
    
    public void deactivateAll() {
        dao.deactivateAll();
    }
    
    public void setPaused(long id, boolean paused) {
        dao.setPaused(id, paused);
    }
    
    public void updateProgress(long id, int progress) {
        dao.updateProgress(id, progress, System.currentTimeMillis());
    }
    
    public void markCompleted(long id) {
        dao.markCompleted(id, System.currentTimeMillis());
    }
}
