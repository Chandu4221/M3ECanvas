package dev.chandradsl.m3ecanvas.editor.persistence

import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import java.io.File

/**
 * JVM implementation of [ProjectRepository]. Stores the project as a
 * pretty-printed JSON file at ~/.m3ecanvas/project.json.
 */
class FileProjectRepository(
    private val file: File = defaultFile()
) : ProjectRepository {

    override fun save(project: M3EProject) {
        file.parentFile?.mkdirs()
        file.writeText(text = M3EJson.encodeToString(value = project))
    }

    override fun load(): M3EProject? {
        if (!file.exists()) return null
        return try {
            M3EJson.decodeFromString<M3EProject>(string = file.readText())
        } catch (_: Exception) {
            null
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