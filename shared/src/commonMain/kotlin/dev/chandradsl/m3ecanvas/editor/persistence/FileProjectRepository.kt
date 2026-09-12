package dev.chandradsl.m3ecanvas.editor.persistence

import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * JVM implementation of [ProjectRepository]. Stores the project as a
 * pretty-printed JSON file at ~/.m3ecanvas/project.json.
 */
class FileProjectRepository(
    private val file: File = defaultFile()
) : ProjectRepository {

    override suspend fun save(project: M3EProject): Unit = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.writeText(text = M3EJson.encodeToString(value = project))
    }

    override suspend fun load(): LoadResult = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext LoadResult.NotFound
        try {
            val text = file.readText()
            val project = M3EJson.decodeFromString<M3EProject>(string = text)
            LoadResult.Success(project = project)
        } catch (e: Throwable) {
            LoadResult.Corrupted(reason = e.message ?: "Failed to read or parse project file", cause = e)
        }
    }

    companion object {
        /** Stable location independent of the launch directory. */
        fun defaultFile(): File {
            val dir = File(System.getProperty("user.home"), ".m3ecanvas")
            return File(dir, "project.json")
        }
    }
}