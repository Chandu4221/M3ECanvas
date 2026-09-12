package dev.chandradsl.m3ecanvas.editor.persistence

import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class RecentProjectEntry(
    val name: String,
    val path: String,
    val lastOpenedTimestamp: Long
)

/**
 * Manages periodic recovery autosave snapshots and the most-recently-used (MRU) projects list.
 */
class AutosaveManager(
    private val autosaveFile: File = defaultAutosaveFile(),
    private val recentsFile: File = defaultRecentsFile(),
    val serializer: ProjectSerializer = ProjectSerializer.default
) {

    suspend fun saveAutosave(project: M3EProject): Unit = withContext(Dispatchers.IO) {
        autosaveFile.parentFile?.mkdirs()
        autosaveFile.writeText(serializer.encodeToString(project))
    }

    suspend fun loadAutosave(): LoadResult = withContext(Dispatchers.IO) {
        if (!autosaveFile.exists()) return@withContext LoadResult.NotFound
        try {
            val text = autosaveFile.readText()
            val project = serializer.decodeFromString(text)
            LoadResult.Success(project)
        } catch (e: Throwable) {
            LoadResult.Corrupted(reason = e.message ?: "Corrupted autosave file", cause = e)
        }
    }

    suspend fun clearAutosave(): Unit = withContext(Dispatchers.IO) {
        if (autosaveFile.exists()) {
            autosaveFile.delete()
        }
    }

    suspend fun hasAutosave(): Boolean = withContext(Dispatchers.IO) {
        autosaveFile.exists() && autosaveFile.length() > 0
    }

    suspend fun addRecentProject(name: String, path: String): Unit = withContext(Dispatchers.IO) {
        recentsFile.parentFile?.mkdirs()
        val current = getRecentProjects().filterNot { it.path == path }
        val updated = listOf(RecentProjectEntry(name, path, System.currentTimeMillis())) + current
        val capped = updated.take(10)
        recentsFile.writeText(serializer.json.encodeToString(capped))
    }

    suspend fun getRecentProjects(): List<RecentProjectEntry> = withContext(Dispatchers.IO) {
        if (!recentsFile.exists()) return@withContext emptyList()
        try {
            serializer.json.decodeFromString(recentsFile.readText())
        } catch (_: Throwable) {
            emptyList()
        }
    }

    companion object {
        fun defaultAutosaveFile(): File {
            val dir = File(System.getProperty("user.home"), ".m3ecanvas")
            return File(dir, "autosave.json")
        }

        fun defaultRecentsFile(): File {
            val dir = File(System.getProperty("user.home"), ".m3ecanvas")
            return File(dir, "recents.json")
        }

        val default = AutosaveManager()
    }
}
