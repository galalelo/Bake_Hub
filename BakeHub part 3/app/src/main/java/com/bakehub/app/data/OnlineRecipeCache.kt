package com.bakehub.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Small on-device cache of TheMealDB recipes the user has searched for, browsed or saved, so they
 * can still be opened with no connection. Firestore recipes are cached by Firestore itself.
 * Keeps the most recent [MAX_ENTRIES] recipes (oldest evicted first).
 */
object OnlineRecipeCache {
    private const val FILE = "online_recipe_cache"
    private const val KEY_ORDER = "__order"
    private const val MAX_ENTRIES = 100

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun put(recipe: Recipe) {
        val p = prefs ?: return
        val order = readOrder(p).filter { it != recipe.id }.toMutableList()
        order.add(recipe.id)
        val editor = p.edit()
        while (order.size > MAX_ENTRIES) editor.remove(order.removeAt(0))
        editor.putString(recipe.id, RecipeJsonCodec.encode(recipe))
        editor.putString(KEY_ORDER, order.joinToString(","))
        editor.apply()
    }

    fun putAll(recipes: List<Recipe>) = recipes.forEach { put(it) }

    @Synchronized
    fun get(id: String): Recipe? =
        prefs?.getString(id, null)?.let { RecipeJsonCodec.decode(it) }

    private fun readOrder(p: SharedPreferences): List<String> =
        p.getString(KEY_ORDER, "").orEmpty().split(",").filter { it.isNotBlank() }
}
