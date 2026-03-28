package com.najmi.falco.data.local.dao

import androidx.room.*
import com.najmi.falco.data.local.entity.PaperStanceEntity

@Dao
interface PaperStanceDao {
    @Query("SELECT * FROM paper_stances WHERE verdictId = :verdictId")
    suspend fun getStancesForVerdict(verdictId: String): List<PaperStanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stances: List<PaperStanceEntity>)

    @Query("DELETE FROM paper_stances WHERE verdictId = :verdictId")
    suspend fun deleteForVerdict(verdictId: String)
}
