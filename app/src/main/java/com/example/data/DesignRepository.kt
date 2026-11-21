package com.example.data

import com.example.model.DesignProject
import com.example.model.KanvoSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DesignRepository(private val dao: DesignProjectDao) {

    val allProjects: Flow<List<DesignProject>> = dao.observeAllProjects().map { entities ->
        entities.map { entity -> entity.toDomain() }
    }

    suspend fun getProject(id: String): DesignProject? {
        return dao.getProjectById(id)?.toDomain()
    }

    suspend fun saveProject(project: DesignProject) {
        dao.upsertProject(project.toEntity())
    }

    suspend fun deleteProject(id: String) {
        dao.deleteProjectById(id)
    }

    private fun DesignProjectEntity.toDomain(): DesignProject {
        return DesignProject(
            id = id,
            title = title,
            aspectRatioLabel = aspectRatioLabel,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            pages = KanvoSerializer.decodePages(pagesJson),
            updatedAt = updatedAt,
            thumbnailPath = thumbnailPath
        )
    }

    private fun DesignProject.toEntity(): DesignProjectEntity {
        return DesignProjectEntity(
            id = id,
            title = title,
            aspectRatioLabel = aspectRatioLabel,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            pagesJson = KanvoSerializer.encodePages(pages),
            updatedAt = updatedAt,
            thumbnailPath = thumbnailPath
        )
    }
}
