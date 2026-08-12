package com.denham.skyline.core

/**
 * Xtream categories are free-text per provider, so "is this sports/kids?" comes
 * down to name-keyword matching. Kept in one place so the phone and TV UIs
 * classify categories identically.
 */
object CategoryKeywords {

    val SPORT = listOf(
        "sport", "football", "soccer", "f1", "formula", "racing", "ufc", "fight",
        "boxing", "nba", "nfl", "cricket", "tennis", "golf", "rugby",
    )

    val KIDS = listOf("kid", "children", "cartoon", "junior")

    fun isSports(name: String): Boolean =
        SPORT.any { name.contains(it, ignoreCase = true) }

    fun isKids(name: String): Boolean =
        KIDS.any { name.contains(it, ignoreCase = true) }
}
