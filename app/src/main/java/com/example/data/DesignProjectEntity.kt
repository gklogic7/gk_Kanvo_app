package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "design_projects")
data class DesignProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val aspectRatioLabel: String,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val pagesJson: String,
    val updatedAt: Long,
    val thumbnailPath: String? = null
)
