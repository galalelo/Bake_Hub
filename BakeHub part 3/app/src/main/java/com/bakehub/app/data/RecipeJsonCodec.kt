package com.bakehub.app.data

import com.google.gson.Gson

/** JSON (de)serialisation for [Recipe] used by the offline cache. Pure JVM so it is unit-testable. */
object RecipeJsonCodec {
    private val gson = Gson()

    fun encode(recipe: Recipe): String = gson.toJson(recipe)

    fun decode(json: String): Recipe? =
        runCatching { gson.fromJson(json, Recipe::class.java) }
            .getOrNull()
            ?.takeIf { runCatching { it.id.isNotBlank() && it.title.isNotBlank() }.getOrDefault(false) }
}
