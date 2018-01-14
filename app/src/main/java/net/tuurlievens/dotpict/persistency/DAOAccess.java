package net.tuurlievens.dotpict.persistency;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import net.tuurlievens.dotpict.models.Save;

import java.util.List;

@Dao
public interface DAOAccess {

    @Insert
    void insert(Save save);

    @Query("SELECT id, name FROM Save")
    List<Save> fetchAllData();

    @Query("SELECT * FROM Save WHERE id =:id")
    Save getSingleRecord(int id);

    @Update
    void update(Save save);

    @Delete
    void delete(Save save);
}
