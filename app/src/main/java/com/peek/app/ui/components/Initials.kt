package com.peek.app.ui.components

/** Up to two initials for a monogram avatar ("Weekend plans" -> "WP"); "•" when none. */
fun initialsOf(name: String): String =
    name.split(Regex("\\s+"))
        .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifEmpty { "•" }
