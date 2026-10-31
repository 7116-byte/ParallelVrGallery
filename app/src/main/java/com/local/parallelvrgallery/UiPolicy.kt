package com.local.parallelvrgallery

/** Pure UI/update rules, independent of Android and generation queues. */
internal fun boundedColumns(requested: Int, widthDp: Int, fontScale: Float, cards: Boolean): Int {
    val minimum = (if (cards) 148f else 84f) * fontScale.coerceAtLeast(1f)
    val capacity = ((widthDp - 32).coerceAtLeast(1) / minimum).toInt().coerceAtLeast(1)
    return requested.coerceIn(1, minOf(8, capacity))
}

internal fun compareVersionTags(left: String, right: String): Int {
    val a = left.trim().removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
    val b = right.trim().removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
    for (index in 0 until maxOf(a.size, b.size)) {
        val compared = a.getOrElse(index) { 0 }.compareTo(b.getOrElse(index) { 0 })
        if (compared != 0) return compared
    }
    return 0
}

/** Extractor flags and codec flags use different bit assignments. Never forward encrypted/partial flags. */
internal fun muxerSampleFlags(extractorFlags: Int): Int = if (extractorFlags and 1 != 0) 1 else 0

internal fun sameSigningDigests(installed: Set<String>, downloaded: Set<String>): Boolean =
    installed.isNotEmpty() && downloaded.isNotEmpty() && installed == downloaded

internal fun isNewerApkVersion(tag: String, code: Long, installedTag: String, installedCode: Long): Boolean =
    compareVersionTags(tag, installedTag) > 0 && code > installedCode

internal data class UiNavigation(
    val tab: String = "all", val generatedTab: String = "images", val album: String? = null,
    val version: String? = null, val index: Int = 0, val offset: Int = 0,
    val generatedIndex: Int = 0, val generatedOffset: Int = 0, val videoIndex: Int = 0, val videoOffset: Int = 0,
    val versionIndex: Int = 0, val versionOffset: Int = 0,
    val allColumns: Int = 5, val albumColumns: Int = 5, val detailColumns: Int = 5, val generatedColumns: Int = 5,
) {
    fun values(): Map<String, String> = mapOf(
        "tab" to tab, "generatedTab" to generatedTab, "album" to album.orEmpty(), "version" to version.orEmpty(),
        "index" to "$index", "offset" to "$offset", "generatedIndex" to "$generatedIndex", "generatedOffset" to "$generatedOffset",
        "videoIndex" to "$videoIndex", "videoOffset" to "$videoOffset", "versionIndex" to "$versionIndex", "versionOffset" to "$versionOffset",
        "allColumns" to "$allColumns", "albumColumns" to "$albumColumns", "detailColumns" to "$detailColumns", "generatedColumns" to "$generatedColumns",
    )
    companion object {
        fun from(values: Map<String, String>): UiNavigation {
            fun position(key: String) = values[key]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            fun columns(key: String) = (values[key]?.toIntOrNull() ?: 5).coerceIn(1, 8)
            return UiNavigation(
                tab = values["tab"].takeIf { it in setOf("all", "albums", "generated") } ?: "all",
                generatedTab = values["generatedTab"].takeIf { it in setOf("images", "videos") } ?: "images",
                album = values["album"]?.takeIf { it.isNotBlank() }, version = values["version"]?.takeIf { it.isNotBlank() },
                index = position("index"), offset = position("offset"), generatedIndex = position("generatedIndex"), generatedOffset = position("generatedOffset"),
                videoIndex = position("videoIndex"), videoOffset = position("videoOffset"), versionIndex = position("versionIndex"), versionOffset = position("versionOffset"),
                allColumns = columns("allColumns"), albumColumns = columns("albumColumns"), detailColumns = columns("detailColumns"), generatedColumns = columns("generatedColumns"),
            )
        }
    }
}
