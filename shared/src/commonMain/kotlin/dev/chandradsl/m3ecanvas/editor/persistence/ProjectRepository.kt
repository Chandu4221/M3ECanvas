package dev.chandradsl.m3ecanvas.editor.persistence

import dev.chandradsl.m3ecanvas.domain.model.M3EProject
import kotlinx.serialization.json.Json

/**
 * Storage abstraction for [M3EProject]. Common code depends only on this
 * interface; each platform supplies an implementation.
 */
interface ProjectRepository {

    /** Persists [project], replacing any previous save. */
    fun save(project: M3EProject)

    /** Returns the last saved project, or null if none exists or it is unreadable. */
    fun load(): M3EProject?
}

/**
 * Shared Json configuration for project files.
 *
 * - prettyPrint: human-readable, diff-friendly files
 * - encodeDefaults: explicit, stable files even for default values
 * - ignoreUnknownKeys: tolerate older or newer files gracefully
 * - classDiscriminator "kind": avoids any clash with CanvasNode.type
 */
val M3EJson: Json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    classDiscriminator = "kind"
}