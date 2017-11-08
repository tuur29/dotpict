package net.tuurlievens.dotpict.saves;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Save.class}, version = 1)
public abstract class SavesDatabase extends RoomDatabase {
    public abstract DAOAccess daoAccess();
}
