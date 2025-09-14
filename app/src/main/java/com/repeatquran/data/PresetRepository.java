package com.repeatquran.data;

import android.content.Context;

import com.repeatquran.data.db.PresetDao;
import com.repeatquran.data.db.PresetEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;

import java.util.List;

public class PresetRepository {
    private final PresetDao dao;

    public PresetRepository(Context context) {
        this.dao = RepeatQuranDatabase.get(context).presetDao();
    }

    public long insert(PresetEntity e) { return dao.insert(e); }
    public int update(PresetEntity e) { return dao.update(e); }
    public int delete(PresetEntity e) { return dao.delete(e); }
    public List<PresetEntity> getAll() { return dao.getAllOrdered(); }
}

