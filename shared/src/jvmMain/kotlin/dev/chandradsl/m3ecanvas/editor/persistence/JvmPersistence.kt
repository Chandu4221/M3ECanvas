package dev.chandradsl.m3ecanvas.editor.persistence

import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * JVM implementation of [ProjectRepository]. Stores the project as a
 * pretty-printed JSON file at ~/.m3ecanvas/project.json.
 * Writes are crash-safe and atomic via temporary file swap.
 */
class FileProjectRepository(
    private val file: File = defaultFile(),
    val serializer: ProjectSerializer = ProjectSerializer.default
) : ProjectRepository {

    override val storageLocation: String
        get() = file.absolutePath

    override suspend fun save(project: M3EProject): Unit = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        val tempFile = File(file.parentFile, "${file.name}.${UUID.randomUUID()}.tmp")
        tempFile.writeText(serializer.encodeToString(project = project))
        try {
            Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: Throwable) {
            Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    override suspend fun load(): LoadResult = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext LoadResult.NotFound
        try {
            val text = file.readText()
            val project = serializer.decodeFromString(string = text)
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

/**
 * JVM implementation of [AutosaveManager].
 * Writes are crash-safe and atomic via temporary file swap.
 */
class JvmAutosaveManager(
    private val autosaveFile: File = defaultAutosaveFile(),
    private val recentsFile: File = defaultRecentsFile(),
    val serializer: ProjectSerializer = ProjectSerializer.default
) : AutosaveManager {

    override suspend fun saveAutosave(project: M3EProject): Unit = withContext(Dispatchers.IO) {
        autosaveFile.parentFile?.mkdirs()
        val tempFile = File(autosaveFile.parentFile, "${autosaveFile.name}.${UUID.randomUUID()}.tmp")
        tempFile.writeText(serializer.encodeToString(project))
        try {
            Files.move(
                tempFile.toPath(),
                autosaveFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: Throwable) {
            Files.move(
                tempFile.toPath(),
                autosaveFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    override suspend fun loadAutosave(): LoadResult = withContext(Dispatchers.IO) {
        if (!autosaveFile.exists()) return@withContext LoadResult.NotFound
        try {
            val text = autosaveFile.readText()
            val project = serializer.decodeFromString(text)
            LoadResult.Success(project)
        } catch (e: Throwable) {
            LoadResult.Corrupted(reason = e.message ?: "Corrupted autosave file", cause = e)
        }
    }

    override suspend fun clearAutosave(): Unit = withContext(Dispatchers.IO) {
        if (autosaveFile.exists()) {
            autosaveFile.delete()
        }
    }

    override suspend fun hasAutosave(): Boolean = withContext(Dispatchers.IO) {
        autosaveFile.exists() && autosaveFile.length() > 0
    }

    override suspend fun addRecentProject(name: String, path: String): Unit = withContext(Dispatchers.IO) {
        recentsFile.parentFile?.mkdirs()
        val current = getRecentProjects().filterNot { it.path == path }
        val updated = listOf(RecentProjectEntry(name, path, System.currentTimeMillis())) + current
        val capped = updated.take(10)
        val tempFile = File(recentsFile.parentFile, "${recentsFile.name}.${UUID.randomUUID()}.tmp")
        tempFile.writeText(serializer.json.encodeToString(capped))
        try {
            Files.move(
                tempFile.toPath(),
                recentsFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: Throwable) {
            Files.move(
                tempFile.toPath(),
                recentsFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    override suspend fun getRecentProjects(): List<RecentProjectEntry> = withContext(Dispatchers.IO) {
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

        val default = JvmAutosaveManager()
    }
}

actual fun createDefaultProjectRepository(): ProjectRepository = FileProjectRepository()

actual fun createDefaultAutosaveManager(): AutosaveManager = JvmAutosaveManager.default

actual fun getDefaultProjectLocation(): String = FileProjectRepository.defaultFile().absolutePath

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
