package dev.chandradsl.m3ecanvas.editor.history

/**
 * Bounded history stack supporting undo and redo operations.
 *
 * Implemented using [ArrayDeque] with a maximum depth limit to avoid unbounded
 * memory growth during long editing sessions.
 *
 * @param maxDepth The maximum number of undo steps preserved (default: 50).
 */
class HistoryStack<T>(
    private val maxDepth: Int = 50
) {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    val undoCount: Int
        get() = undoStack.size

    val redoCount: Int
        get() = redoStack.size

    /**
     * Records a new state. Clears the redo stack and bounds the undo stack to [maxDepth].
     */
    fun push(state: T) {
        redoStack.clear()
        undoStack.addLast(state)
        if (undoStack.size > maxDepth) {
            undoStack.removeFirst()
        }
    }

    /**
     * Undoes the last operation.
     * Pushes [currentState] onto the redo stack and returns the previous state from the undo stack,
     * or null if no undo steps remain.
     */
    fun undo(currentState: T): T? {
        if (undoStack.isEmpty()) return null
        val previousState = undoStack.removeLast()
        redoStack.addLast(currentState)
        return previousState
    }

    /**
     * Redoes the last undone operation.
     * Pushes [currentState] onto the undo stack and returns the next state from the redo stack,
     * or null if no redo steps remain.
     */
    fun redo(currentState: T): T? {
        if (redoStack.isEmpty()) return null
        val nextState = redoStack.removeLast()
        undoStack.addLast(currentState)
        return nextState
    }

    /** Returns a snapshot list of states currently available on the undo stack (oldest first). */
    fun getUndoList(): List<T> = undoStack.toList()

    /** Returns a snapshot list of states currently available on the redo stack (next redo first). */
    fun getRedoList(): List<T> = redoStack.toList().reversed()

    /**
     * Undoes [steps] operations in sequence.
     * Returns the target state reached, or null if [steps] <= 0 or undo stack is empty.
     */
    fun undoSteps(steps: Int, currentState: T): T? {
        if (steps <= 0 || undoStack.isEmpty()) return null
        var current = currentState
        var result: T? = null
        val count = minOf(steps, undoStack.size)
        for (i in 0 until count) {
            val prev = undo(current) ?: break
            current = prev
            result = prev
        }
        return result
    }

    /**
     * Redoes [steps] operations in sequence.
     * Returns the target state reached, or null if [steps] <= 0 or redo stack is empty.
     */
    fun redoSteps(steps: Int, currentState: T): T? {
        if (steps <= 0 || redoStack.isEmpty()) return null
        var current = currentState
        var result: T? = null
        val count = minOf(steps, redoStack.size)
        for (i in 0 until count) {
            val next = redo(current) ?: break
            current = next
            result = next
        }
        return result
    }

    /**
     * Clears both undo and redo histories.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    /**
     * Transforms all states currently held in both undo and redo stacks in-place.
     * Useful for updating environmental or viewport settings without modifying the history structure.
     */
    fun map(transform: (T) -> T) {
        val newUndo = undoStack.map(transform)
        undoStack.clear()
        undoStack.addAll(newUndo)

        val newRedo = redoStack.map(transform)
        redoStack.clear()
        redoStack.addAll(newRedo)
    }
}
