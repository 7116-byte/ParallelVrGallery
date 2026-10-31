package com.local.parallelvrgallery

import android.content.Context

/** UI-only recovery. Never persists Viewer/VR activation or starts a generation worker. */
internal class NavigationStore(context: Context) {
    private val prefs = context.getSharedPreferences("navigation_v1", Context.MODE_PRIVATE)

    fun load(): UiNavigation = UiNavigation.from(prefs.all.mapValues { it.value.toString() })

    fun save(value: UiNavigation) {
        val edit = prefs.edit()
        value.values().forEach { (key, text) -> edit.putString(key, text) }
        edit.apply()
    }
}
