package dev.chandradsl.m3ecanvas.editor.persistence

import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import kotlinx.serialization.json.Json

sealed interface LoadResult {
    data class Success(val project: M3EProject) : LoadResult
    data object NotFound : LoadResult
    data class Corrupted(val reason: String, val cause: Throwable? = null) : LoadResult
}

/**
 * Storage abstraction for [M3EProject]. Common code depends only on this
 * interface; each platform supplies an implementation.
 */
interface ProjectRepository {

    /** Persists [project], replacing any previous save. */
    suspend fun save(project: M3EProject)

    /** Returns the result of loading the saved project. */
    suspend fun load(): LoadResult
}

private val DEFAULT_M3E_JSON: Json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    classDiscriminator = "kind"
}

/**
 * Serializer service wrapping Kotlinx Serialization Json for [M3EProject] and components.
 * Can be instantiated with custom configurations or mocked in tests.
 */
open class ProjectSerializer(
    val json: Json = DEFAULT_M3E_JSON
) {
    open fun encodeToString(project: M3EProject): String = json.encodeToString(project)
    open fun decodeFromString(string: String): M3EProject = json.decodeFromString(string)

    companion object {
        val defaultJson: Json = DEFAULT_M3E_JSON
        val default: ProjectSerializer by lazy { ProjectSerializer(DEFAULT_M3E_JSON) }
    }
}

val M3EJson: Json
    get() = ProjectSerializer.defaultJson