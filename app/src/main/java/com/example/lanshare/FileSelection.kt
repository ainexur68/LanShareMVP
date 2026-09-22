package com.example.lanshare

internal fun appendUniqueSharedFiles(
    existing: List<SharedFile>,
    additions: List<SharedFile>
): List<SharedFile> = appendUniqueByKey(existing, additions) { it.uri.toString() }

internal fun <T> appendUniqueByKey(
    existing: List<T>,
    additions: List<T>,
    key: (T) -> String
): List<T> {
    val knownKeys = existing.mapTo(mutableSetOf(), key)
    return existing + additions.filter { knownKeys.add(key(it)) }
}
