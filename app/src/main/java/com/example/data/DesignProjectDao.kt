package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DesignProjectDao {
    @Query("SELECT * FROM design_projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<DesignProjectEntity>>

    @Query("SELECT * FROM design_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): DesignProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: DesignProjectEntity)

    @Query("DELETE FROM design_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}
