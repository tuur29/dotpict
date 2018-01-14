package net.tuurlievens.dotpict.persistency;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import net.tuurlievens.dotpict.models.Save;

@Database(entities = {Save.class}, version = 1)
public abstract class SavesDatabase extends RoomDatabase {
    public abstract DAOAccess daoAccess();
}
