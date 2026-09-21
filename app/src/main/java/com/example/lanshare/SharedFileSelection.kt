package com.example.lanshare

/** Pure selection semantics used by both the Android entry point and JVM tests. */
internal object SharedFileSelection {
    fun <T> merge(
        existing: List<T>,
        incoming: List<T>,
        replaceExisting: Boolean,
        key: (T) -> String
    ): List<T> {
        val source = if (replaceExisting) incoming else existing + incoming
        val seen = HashSet<String>(source.size)
        return source.filter { seen.add(key(it)) }
    }
}
