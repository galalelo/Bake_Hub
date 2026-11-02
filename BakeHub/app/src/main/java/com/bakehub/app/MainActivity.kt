package com.bakehub.app

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bakehub.app.data.*
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var container: FrameLayout
    private lateinit var bottomNav: LinearLayout
    private var route = "onboarding"
    private var selectedRecipe: Recipe = MockData.recipes.first()
    private val history = java.util.ArrayDeque<String>()
    private val brown get() = ContextCompat.getColor(this, R.color.dark_brown)
    private val grey get() = ContextCompat.getColor(this, R.color.grey_text)
    private val terra get() = ContextCompat.getColor(this, R.color.terracotta)
    private val white get() = ContextCompat.getColor(this, R.color.white)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.screen_container)
        bottomNav = findViewById(R.id.bottom_nav)
        findViewById<View>(R.id.nav_home).setOnClickListener { show("home") }
        findViewById<View>(R.id.nav_search).setOnClickListener { show("search") }
        findViewById<View>(R.id.nav_box).setOnClickListener { show("recipe_box") }
        findViewById<View>(R.id.nav_grocery).setOnClickListener { show("grocery") }
        findViewById<View>(R.id.nav_profile).setOnClickListener { show("profile") }
        if (AuthRepository.isLoggedIn) { activateFirebase(); show("home", false) } else show("onboarding", false)
    }

    override fun onBackPressed() {
        if (history.isNotEmpty()) show(history.removeLast(), false) else super.onBackPressed()
    }

    private fun activateFirebase() {
        UserRepository.startListening()
        RecipeRepository.startListening()
        lifecycleScope.launch { runCatching { RecipeRepository.seedIfEmpty() } }
    }

    private fun show(next: String, remember: Boolean = true) {
        if (remember && next != route) history.addLast(route)
        route = next
        bottomNav.visibility = if (next in setOf("home", "search", "recipe_box", "grocery", "profile")) View.VISIBLE else View.GONE
        container.removeAllViews()
        val layout = resources.getIdentifier("screen_$next", "layout", packageName)
        val root = layoutInflater.inflate(layout, container, false)
        container.addView(root)
        val content = root.findViewById<LinearLayout>(R.id.content)
        when (next) {
            "onboarding" -> onboarding(content)
            "login" -> login(content)
            "signup" -> signup(content)
            "home" -> home(content)
            "search" -> search(content)
            "recipe_box" -> recipeBox(content)
            "grocery" -> grocery(content)
            "profile" -> profile(content)
            "create_recipe" -> createRecipe(content)
            "settings" -> settings(content)
            "recipe_detail" -> recipeDetail(content)
            "bake_mode" -> bakeMode(content)
        }
    }

    private fun onboarding(c: LinearLayout) {
        c.gravity = Gravity.CENTER_HORIZONTAL; spacer(c, 90); text(c, "🥖", 48, brown, center = true)
        text(c, "BakeHub", 34, brown, true, center = true, top = 20)
        text(c, "Bake smarter, together.", 16, grey, center = true, top = 4, bottom = 48)
        button(c, "Get Started") { show("login") }; outline(c, "I already have an account", 12) { show("login") }
    }

    private fun login(c: LinearLayout) {
        text(c, "Welcome back", 26, brown, true); text(c, "Log in to continue baking", 14, grey, top = 4, bottom = 24)
        val email = edit(c, "Email address"); val password = edit(c, "Password", true, 10); val status = text(c, "", 13, terra, top = 8); status.visibility = View.GONE
        button(c, "Log In", 20) { lifecycleScope.launch { try { AuthRepository.login(email.text.toString(), password.text.toString()); activateFirebase(); show("home") } catch (e: Exception) { status.text = e.message ?: "Login failed"; status.visibility = View.VISIBLE } } }
        outline(c, "Continue with Google") { status.text = "Google Sign-In must be enabled in Firebase Console and added here with your OAuth client ID."; status.visibility = View.VISIBLE }
        text(c, "New here? Create an account", 14, terra, center = true, top = 24).setOnClickListener { show("signup") }
    }

    private fun signup(c: LinearLayout) {
        outline(c, "‹ Back") { onBackPressed() }; text(c, "Create your account", 24, brown, true, top = 12, bottom = 20)
        val name = edit(c, "Display name"); val email = edit(c, "Email address", top = 10); val password = edit(c, "Password (6+ characters)", true, 10); val status = text(c, "", 13, terra, top = 8); status.visibility = View.GONE
        button(c, "Create Account", 20) { lifecycleScope.launch { try { AuthRepository.signUp(name.text.toString(), email.text.toString(), password.text.toString()); activateFirebase(); show("home") } catch (e: Exception) { status.text = e.message ?: "Sign-up failed"; status.visibility = View.VISIBLE } } }
    }

    private fun currentProfile() = UserRepository.profile.value ?: MockData.currentUser
    private fun currentRecipes() = RecipeRepository.recipes.value.ifEmpty { MockData.recipes }

    private fun home(c: LinearLayout) {
        val u = currentProfile(); text(c, "Good morning, ${u.displayName.substringBefore(' ')}", 22, brown, true); text(c, "🔥 ${u.streakCount}-day streak · Lvl ${u.level}", 14, terra, top = 2, bottom = 16)
        val search = edit(c, "Search recipes, ingredients…"); search.isFocusable = false; search.setOnClickListener { show("search") }
        text(c, "Recommended for you", 17, brown, true, top = 22, bottom = 8)
        currentRecipes().take(4).forEach { recipeCard(c, it) }
    }

    private fun search(c: LinearLayout) {
        val input = edit(c, "Search recipes, ingredients…"); val count = text(c, "", 14, grey, top = 14, bottom = 10); val results = LinearLayout(this); results.orientation = LinearLayout.VERTICAL; c.addView(results)
        fun render() {
            val q = input.text.toString().trim()
            results.removeAllViews()
            val local = currentRecipes().filter { q.isBlank() || it.title.contains(q, true) || it.category.contains(q, true) }
            count.text = "${local.size} recipes from Firestore"
            local.forEach { recipeRow(results, it, "Saved in Firebase") }
            if (q.isNotBlank()) {
                val loading = text(results, "Searching online recipes…", 14, grey, top = 8)
                lifecycleScope.launch {
                    val online = runCatching { RecipeRepository.searchOnline(q) }.getOrDefault(emptyList())
                    loading.visibility = View.GONE
                    count.text = "${local.size + online.size} recipes · Firestore + online"
                    online.forEach { recipeRow(results, it, "Online · open source link for attribution") }
                }
            }
        }
        input.addTextChangedListener(SimpleWatcher { render() }); render()
    }

    private fun recipeBox(c: LinearLayout) {
        val savedIds = UserRepository.savedRecipeIds.value; text(c, "My Recipe Box", 22, brown, true); text(c, "${savedIds.size} saved · Firebase Firestore sync", 14, grey, top = 2, bottom = 16)
        text(c, "+ New recipe", 15, terra, true, bottom = 16).setOnClickListener { show("create_recipe") }
        currentRecipes().filter { it.id in savedIds }.forEach { recipeRow(c, it, "Saved in Firestore") }
    }

    private fun grocery(c: LinearLayout) { text(c, "This Week's List", 22, brown, true); text(c, "Your grocery list", 14, grey, top = 2, bottom = 16); MockData.groceryItems.forEach { item -> val cb = CheckBox(this); cb.text = "${item.name} — ${item.quantity}"; cb.setTextColor(brown); c.addView(cb) }; button(c, "Share List", 20) { shareList() } }
    private fun shareList() { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "BakeHub grocery list") }, "Share list")) }

    private fun profile(c: LinearLayout) { val u = currentProfile(); c.gravity = Gravity.CENTER_HORIZONTAL; text(c, "◯", 70, grey, center = true); text(c, u.displayName, 20, brown, true, center = true, top = 10); text(c, "Level ${u.level} · ${u.xp} XP", 14, terra, center = true); text(c, "🔥 Current streak: ${u.streakCount} days", 16, brown, top = 18); text(c, "Badges", 17, brown, true, top = 22, bottom = 8); MockData.badges.forEach { text(c, "${if (it.unlocked) "🏅" else "🔒"} ${it.name}", 14, if (it.unlocked) brown else grey, top = 4) }; text(c, "${u.bakesCompleted} bakes completed · ${u.recipesShared} recipes shared", 14, grey, top = 24); outline(c, "⚙ Settings", 22) { show("settings") } }

    private fun createRecipe(c: LinearLayout) {
        outline(c, "‹ Back") { onBackPressed() }; text(c, "New Recipe", 22, brown, true, top = 12); val title = edit(c, "Recipe title", top = 16); text(c, "Ingredients", 16, brown, true, top = 20, bottom = 8); val ingredient = edit(c, "e.g. 2 cups flour"); text(c, "Steps", 16, brown, true, top = 20, bottom = 8); val step = edit(c, "Describe the first step"); val status = text(c, "", 13, terra, top = 8); status.visibility = View.GONE
        button(c, "Publish Recipe", 20) { val r = Recipe("user-${UUID.randomUUID()}", title.text.toString().ifBlank { "Untitled Recipe" }, AuthRepository.currentUser?.displayName ?: "Baker", "User", "Other", Difficulty.EASY, 0, 0, 1, 0f, 0, Nutrition(0, 0f, 0f, 0f), listOf(Ingredient(ingredient.text.toString(), "1", "")), listOf(RecipeStep(1, "Step 1", step.text.toString()))); lifecycleScope.launch { try { RecipeRepository.addRecipe(r); show("recipe_box") } catch (e: Exception) { status.text = e.message ?: "Could not publish"; status.visibility = View.VISIBLE } } }
    }

    private fun settings(c: LinearLayout) { outline(c, "‹ Back") { onBackPressed() }; text(c, "Settings", 22, brown, true, top = 12, bottom = 20); val units = Switch(this); units.text = "Imperial units"; units.isChecked = currentProfile().unitPreference == "Imperial"; units.setTextColor(brown); units.setOnCheckedChangeListener { _, checked -> lifecycleScope.launch { runCatching { UserRepository.setUnitPreference(if (checked) "Imperial" else "Metric") } } }; c.addView(units); button(c, "Log Out", 30) { AuthRepository.logout(); show("onboarding", false) } }

    private fun recipeDetail(c: LinearLayout) { val r = selectedRecipe; outline(c, "‹ Back") { onBackPressed() }; text(c, r.title, 25, brown, true, top = 12); text(c, "★ ${r.avgRating} · ${r.difficulty.name} · ${r.totalTimeMinutes} min · ${r.servings} servings", 14, grey, top = 6, bottom = 18); text(c, "Ingredients", 17, brown, true); r.ingredients.forEach { text(c, "• ${it.quantity} ${it.unit} ${it.name}".trim(), 15, brown, top = 3) }; button(c, "Start Bake Mode ▶", 20) { show("bake_mode") }; outline(c, if (UserRepository.savedRecipeIds.value.contains(r.id)) "★ Saved to Recipe Box" else "☆ Save to Recipe Box", 10) { lifecycleScope.launch { UserRepository.toggleSaved(r.id); show("recipe_detail", false) } }; if (r.sourceUrl != null) outline(c, "View original recipe") { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.sourceUrl))) } }

    private fun bakeMode(c: LinearLayout) { window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); val r = selectedRecipe; var index = 0; val title = text(c, "", 24, brown, true, center = true, top = 24); val instruction = text(c, "", 15, grey, center = true, top = 12, bottom = 22); val timer = text(c, "", 30, terra, true, center = true); val nav = LinearLayout(this); nav.orientation = LinearLayout.HORIZONTAL; c.addView(nav); val back = outline(nav, "Exit"); val next = button(nav, "Next ▶"); fun render() { val s = r.steps[index]; title.text = "Step ${index + 1}/${r.steps.size}: ${s.title}"; instruction.text = s.instruction; timer.text = s.timerSeconds?.let { "%02d:%02d".format(it / 60, it % 60) } ?: ""; back.text = if (index > 0) "◀ Back" else "Exit"; next.text = if (index < r.steps.lastIndex) "Next ▶" else "Finish ✓" }; back.setOnClickListener { if (index > 0) { index--; render() } else { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); onBackPressed() } }; next.setOnClickListener { if (index < r.steps.lastIndex) { index++; render() } else { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); show("home") } }; render() }

    private fun recipeCard(parent: LinearLayout, r: Recipe) = recipeRow(parent, r)
    private fun recipeRow(parent: LinearLayout, r: Recipe, label: String? = null) { val card = LinearLayout(this); card.orientation = LinearLayout.VERTICAL; card.setBackgroundResource(R.drawable.bg_card); val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, 10); parent.addView(card, lp); text(card, r.title, 16, brown, true); text(card, "${r.source} · ★ ${r.avgRating} · ${r.totalTimeMinutes} min", 14, grey, top = 4); label?.let { text(card, it, 13, grey, top = 3) }; card.setOnClickListener { selectedRecipe = r; show("recipe_detail") } }
    private fun text(p: LinearLayout, value: String, size: Int, color: Int, bold: Boolean = false, center: Boolean = false, top: Int = 0, bottom: Int = 0): TextView { val v = TextView(this); v.text = value; v.textSize = size.toFloat(); v.setTextColor(color); if (bold) v.setTypeface(null, Typeface.BOLD); if (center) v.gravity = Gravity.CENTER; v.setPadding(0, dp(top), 0, dp(bottom)); p.addView(v); return v }
    private fun edit(p: LinearLayout, hint: String, password: Boolean = false, top: Int = 0): EditText { val e = EditText(this); e.hint = hint; e.setTextColor(brown); e.setHintTextColor(grey); e.setSingleLine(true); if (password) e.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(top), 0, 0); p.addView(e, lp); return e }
    private fun button(p: LinearLayout, label: String, top: Int = 0, action: () -> Unit = {}): Button { val b = Button(this); b.text = label; b.setTextColor(white); b.setBackgroundResource(R.drawable.bg_terracotta); b.setOnClickListener { action() }; val lp = LinearLayout.LayoutParams(-1, dp(52)); lp.setMargins(0, dp(top), 0, 0); p.addView(b, lp); return b }
    private fun outline(p: LinearLayout, label: String, top: Int = 0, action: () -> Unit = {}): Button { val b = Button(this); b.text = label; b.setTextColor(brown); b.setOnClickListener { action() }; val lp = LinearLayout.LayoutParams(-1, dp(52)); lp.setMargins(0, dp(top), 0, 0); p.addView(b, lp); return b }
    private fun spacer(p: LinearLayout, h: Int) { p.addView(Space(this), LinearLayout.LayoutParams(1, dp(h))) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
private class SimpleWatcher(private val change: () -> Unit) : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit; override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = change(); override fun afterTextChanged(e: android.text.Editable?) = Unit }
