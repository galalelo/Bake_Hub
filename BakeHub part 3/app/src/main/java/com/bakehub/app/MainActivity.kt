package com.bakehub.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bakehub.app.notifications.AppNotifications
import com.bakehub.app.notifications.NotificationHelper
import com.bakehub.app.util.AppPreferences
import com.bakehub.app.util.LocaleHelper
import com.bakehub.app.util.NetworkMonitor
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bakehub.app.data.*
import kotlinx.coroutines.launch
import java.util.UUID
import com.bumptech.glide.Glide
import android.widget.CheckBox
import android.widget.EditText
import android.app.AlertDialog
import android.widget.LinearLayout



class MainActivity : ComponentActivity() {
    private lateinit var container: FrameLayout
    private lateinit var bottomNav: LinearLayout
    private var route = "onboarding"
    // Stops the Bake Mode timer and screen-on flag; set while Bake Mode is showing.
    private var bakeCleanup: (() -> Unit)? = null
    private var selectedRecipe: Recipe? = null
    private val history = java.util.ArrayDeque<String>()
    private val brown get() = ContextCompat.getColor(this, R.color.dark_brown)
    private val grey get() = ContextCompat.getColor(this, R.color.grey_text)
    private val terra get() = ContextCompat.getColor(this, R.color.terracotta)
    private val white get() = ContextCompat.getColor(this, R.color.white)

    private var groceryItemsJob: kotlinx.coroutines.Job? = null

    // POST_NOTIFICATIONS runtime permission (Android 13+) for real-time recipe notifications.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* pushes start working once granted */ }

    // Multi-language: apply the stored in-app language on Android 8-12 (13+ is handled by the OS).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("route", route)
        // Keep the back stack across activity recreation (language change, rotation, ...).
        outState.putStringArrayList("history", ArrayList(history))
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_main)

        // Edge-to-edge is mandatory when targeting Android 15+, so keep content clear of the
        // status bar, navigation bar, display cutout and on-screen keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(R.id.root)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            insets
        }

        // Predictive-back-safe back handling (the legacy onBackPressed() override is no longer called on Android 16).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (history.isNotEmpty()) {
                    show(history.removeLast(), false)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Offline mode: show a banner whenever the device has no connection.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NetworkMonitor.isOnline.collect { online ->
                    findViewById<View>(R.id.offline_banner).visibility =
                        if (online) View.GONE else View.VISIBLE
                }
            }
        }

        container = findViewById(R.id.screen_container)
        bottomNav = findViewById(R.id.bottom_nav)
        findViewById<View>(R.id.nav_home).setOnClickListener { show("home") }
        findViewById<View>(R.id.nav_search).setOnClickListener { show("search") }
        findViewById<View>(R.id.nav_box).setOnClickListener { show("recipe_box") }
        findViewById<View>(R.id.nav_grocery).setOnClickListener { show("grocery") }
        findViewById<View>(R.id.nav_profile).setOnClickListener { show("profile") }
        if (AuthRepository.isLoggedIn) {
            activateFirebase()
            val restorable = setOf("home", "search", "recipe_box", "grocery", "profile", "settings", "create_recipe")
            // Restore the back stack too (only screens that can be rebuilt without extra state).
            history.clear()
            state?.getStringArrayList("history")?.filter { it in restorable }?.let { history.addAll(it) }
            show(state?.getString("route")?.takeIf { it in restorable } ?: "home", false)
        } else show("onboarding", false)
    }

    private fun activateFirebase() {
        UserRepository.startListening()
        RecipeRepository.startListening()
        setupNotifications()
    }

    private fun setupNotifications() {
        AppNotifications.registerToken()
        AppNotifications.syncSubscription()
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            AppPreferences.notificationsEnabled &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onDestroy() {
        bakeCleanup?.invoke()
        super.onDestroy()
    }

    private fun show(next: String, remember: Boolean = true) {
        bakeCleanup?.invoke()
        bakeCleanup = null
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
        text(c, getString(R.string.app_name), 34, brown, true, center = true, top = 20)
        text(c, getString(R.string.tagline), 16, grey, center = true, top = 4, bottom = 48)
        button(c, getString(R.string.get_started)) { show("login") }; outline(c, getString(R.string.have_account), 12) { show("login") }
    }

    private fun login(c: LinearLayout) {
        text(c, getString(R.string.welcome_back), 26, brown, true); text(c, getString(R.string.login_subtitle), 14, grey, top = 4, bottom = 24)
        val email = edit(c, getString(R.string.email_hint)); val password = edit(c, getString(R.string.password_hint), true, 10); val status = text(c, "", 13, terra, top = 8); status.visibility = View.GONE
        button(c, getString(R.string.log_in), 20) { lifecycleScope.launch { try { AuthRepository.login(email.text.toString(), password.text.toString()); activateFirebase(); history.clear(); show("home", false) } catch (e: Exception) { status.text = e.message ?: getString(R.string.login_failed); status.visibility = View.VISIBLE } } }
        outline(c, getString(R.string.continue_google)) { signInWithGoogle(status) }
        text(c, getString(R.string.new_here), 14, terra, center = true, top = 24).setOnClickListener { show("signup") }
    }

    private fun signup(c: LinearLayout) {
        outline(c, getString(R.string.back_button)) { onBackPressedDispatcher.onBackPressed() }; text(c, getString(R.string.create_account_title), 24, brown, true, top = 12, bottom = 20)
        val name = edit(c, getString(R.string.display_name_hint)); val email = edit(c, getString(R.string.email_hint), top = 10); val password = edit(c, getString(R.string.password_hint_new), true, 10); val status = text(c, "", 13, terra, top = 8); status.visibility = View.GONE
        button(c, getString(R.string.create_account), 20) { lifecycleScope.launch { try { AuthRepository.signUp(name.text.toString(), email.text.toString(), password.text.toString()); activateFirebase(); history.clear(); show("home", false) } catch (e: Exception) { status.text = e.message ?: getString(R.string.signup_failed); status.visibility = View.VISIBLE } } }
    }

    /** The live Firestore profile, or neutral starting values until it has loaded. */
    private fun currentProfile() = UserRepository.profile.value ?: UserProfile(
        displayName = AuthRepository.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Baker",
        skillLevel = "Beginner",
        level = 1,
        xp = 0,
        xpForNextLevel = 500,
        streakCount = 0,
        bakesCompleted = 0,
        recipesShared = 0,
        unitPreference = "Metric"
    )
    private fun currentRecipes() = RecipeRepository.recipes.value



    private fun home(c: LinearLayout) {

        val u = currentProfile()

        text(
            c,
            getString(R.string.hello_user, u.displayName.substringBefore(' ')),
            22,
            brown,
            true
        )

        text(
            c,
            getString(R.string.streak_level, u.streakCount, u.level),
            14,
            terra,
            top = 2,
            bottom = 16
        )

        val search = edit(c, getString(R.string.search_hint))
        search.isFocusable = false
        search.setOnClickListener {
            show("search")
        }

        text(
            c,
            getString(R.string.recommended),
            17,
            brown,
            true,
            top = 22,
            bottom = 8
        )

        // TheMealDB recommendations appear first
        val onlineContainer = LinearLayout(this)
        onlineContainer.orientation = LinearLayout.VERTICAL
        c.addView(onlineContainer)

        lifecycleScope.launch {

            val onlineRecipes = RecipeRepository.getRandomOnlineRecipes(3)

            if (onlineRecipes.isNotEmpty()) {

                text(
                    onlineContainer,
                    getString(R.string.discover_new),
                    17,
                    brown,
                    true,
                    top = 8,
                    bottom = 8
                )

                onlineRecipes.forEach { recipe ->
                    recipeCard(onlineContainer, recipe)
                }
            }

            // Firebase recipes appear underneath
            val community = currentRecipes().take(4)
            if (community.isNotEmpty()) {
                text(
                    onlineContainer,
                    getString(R.string.community),
                    17,
                    brown,
                    true,
                    top = 18,
                    bottom = 8
                )
                community.forEach { recipe ->
                    recipeCard(onlineContainer, recipe)
                }
            }
        }
    }



    private fun search(c: LinearLayout) {
        var onlineJob: kotlinx.coroutines.Job? = null
        val input = edit(c, getString(R.string.search_hint)); val count = text(c, "", 14, grey, top = 14, bottom = 10); val results = LinearLayout(this); results.orientation = LinearLayout.VERTICAL; c.addView(results)
        fun render() {
            val q = input.text.toString().trim()
            onlineJob?.cancel()   // drop stale online results from earlier keystrokes
            results.removeAllViews()
            val local = currentRecipes().filter { q.isBlank() || it.title.contains(q, true) || it.category.contains(q, true) }
            count.text = getString(R.string.recipes_from_firestore, local.size)
            local.forEach { recipeRow(results, it, getString(R.string.saved_in_firebase)) }
            if (q.isNotBlank()) {
                val loading = text(results, getString(R.string.searching_online), 14, grey, top = 8)
                onlineJob = lifecycleScope.launch {
                    val onlineResult = runCatching { RecipeRepository.searchOnline(q) }
                    if (onlineResult.exceptionOrNull() is kotlinx.coroutines.CancellationException) return@launch
                    val online = onlineResult.getOrDefault(emptyList())
                    loading.visibility = View.GONE
                    if (onlineResult.isFailure) text(results, getString(R.string.search_offline_notice), 13, grey, top = 8)
                    count.text = getString(R.string.recipes_firestore_online, local.size + online.size)
                    online.forEach { recipeRow(results, it, getString(R.string.online_attribution)) }
                }
            }
        }
        input.addTextChangedListener(SimpleWatcher { render() }); render()
    }


    private fun recipeBox(c: LinearLayout) {

        val savedIds = UserRepository.savedRecipeIds.value

        text(c, getString(R.string.recipe_box_title), 22, brown, true)
        text(
            c,
            getString(R.string.saved_count_sync, savedIds.size),
            14,
            grey,
            top = 2,
            bottom = 16
        )

        text(c, getString(R.string.new_recipe_link), 15, terra, true, bottom = 16)
            .setOnClickListener {
                show("create_recipe")
            }

        if (savedIds.isEmpty()) {
            text(c, getString(R.string.no_saved), 14, grey)
            return
        }

        // Display saved recipes that are already in Firestore.
        val firestoreRecipes = currentRecipes()
            .filter { it.id in savedIds }

        firestoreRecipes.forEach { recipe ->
            recipeRow(c, recipe, getString(R.string.saved_in_box))
        }

        // Find saved recipes that came from TheMealDB.
        val onlineIds = savedIds.filter {
            it.startsWith("mealdb-")
        }

        if (onlineIds.isNotEmpty()) {

            val loadingText = text(
                c,
                getString(R.string.loading_online),
                14,
                grey,
                top = 12
            )

            lifecycleScope.launch {

                val onlineRecipes: List<Recipe> = onlineIds.mapNotNull { id ->
                    RecipeRepository.getOnlineRecipeById(id)
                }

                loadingText.visibility = View.GONE

                onlineRecipes.forEach { recipe ->
                    recipeRow(c, recipe, getString(R.string.saved_from_mealdb))
                }

                if (firestoreRecipes.isEmpty() && onlineRecipes.isEmpty()) {
                    text(
                        c,
                        getString(R.string.load_saved_failed),
                        14,
                        grey
                    )
                }
            }
        } else if (firestoreRecipes.isEmpty()) {
            text(c, getString(R.string.no_saved_displayable), 14, grey)
        }
    }


    private fun grocery(c: LinearLayout) {

        text(c, getString(R.string.grocery_title), 22, brown, true)
        text(c, getString(R.string.grocery_subtitle), 14, grey, top = 2, bottom = 16)

        val listContainer = LinearLayout(this)
        listContainer.orientation = LinearLayout.VERTICAL
        c.addView(listContainer)

        fun refreshList(items: List<GroceryItem>) {

            listContainer.removeAllViews()

            if (items.isEmpty()) {
                text(
                    listContainer,
                    getString(R.string.grocery_empty),
                    14,
                    grey,
                    bottom = 12
                )
            }

            items.forEach { item ->

                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.setPadding(dp(10), dp(8), dp(10), dp(8))
                row.setBackgroundResource(R.drawable.bg_card)

                val params = LinearLayout.LayoutParams(-1, -2)
                params.bottomMargin = dp(8)
                listContainer.addView(row, params)

                // Checkbox
                val checkbox = CheckBox(this)
                checkbox.text = "${item.name} — ${item.quantity}"
                checkbox.setTextColor(brown)
                checkbox.isChecked = item.checked

                row.addView(
                    checkbox,
                    LinearLayout.LayoutParams(0, -2, 1f)
                )

                checkbox.setOnCheckedChangeListener { _, checked ->

                    lifecycleScope.launch {
                        try {
                            UserRepository.setGroceryItemChecked(
                                item.id,
                                checked
                            )
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                getString(R.string.err_update_item, e.message.orEmpty()),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                // Edit button
                val editButton = outline(row, getString(R.string.edit), 8)

                editButton.layoutParams = LinearLayout.LayoutParams(
                    dp(65), dp(42)
                ).apply {
                    marginStart = dp(4)
                }

                editButton.setOnClickListener {

                    val nameInput = EditText(this)
                    nameInput.setText(item.name)
                    nameInput.hint = getString(R.string.item_name)

                    val quantityInput = EditText(this)
                    quantityInput.setText(item.quantity)
                    quantityInput.hint = getString(R.string.quantity)

                    val layout = LinearLayout(this)
                    layout.orientation = LinearLayout.VERTICAL
                    layout.setPadding(dp(20), dp(8), dp(20), 0)

                    layout.addView(nameInput)
                    layout.addView(quantityInput)

                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.edit_grocery_item))
                        .setView(layout)
                        .setPositiveButton(getString(R.string.save)) { _, _ ->

                            val newName =
                                nameInput.text.toString().trim()

                            val newQuantity =
                                quantityInput.text.toString().trim()

                            if (newName.isNotEmpty() &&
                                newQuantity.isNotEmpty()
                            ) {

                                lifecycleScope.launch {
                                    try {
                                        UserRepository.updateGroceryItem(
                                            item.copy(
                                                name = newName,
                                                quantity = newQuantity
                                            )
                                        )
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.err_edit_item, e.message.orEmpty()),
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }

                            } else {
                                android.widget.Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.enter_both_fields),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show()
                }

                // Delete button
                val deleteButton = outline(row, "✕", 8)

                deleteButton.layoutParams = LinearLayout.LayoutParams(
                    dp(48), dp(42)
                ).apply {
                    marginStart = dp(4)
                }

                deleteButton.setOnClickListener {

                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.delete_grocery_item))
                        .setMessage(getString(R.string.confirm_delete_item, item.name))
                        .setPositiveButton(getString(R.string.delete)) { _, _ ->

                            lifecycleScope.launch {
                                try {
                                    UserRepository.deleteGroceryItem(item.id)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.err_delete_item, e.message.orEmpty()),
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show()
                }
            }
        }

        // Listen for Firestore grocery-list updates
        groceryItemsJob?.cancel()

        groceryItemsJob = lifecycleScope.launch {
            UserRepository.groceryItems.collect { items ->
                refreshList(items)
            }
        }

        // Add grocery item
        button(c, getString(R.string.add_grocery_button), 16) {

            val nameInput = EditText(this)
            nameInput.hint = getString(R.string.item_name)

            val quantityInput = EditText(this)
            quantityInput.hint = getString(R.string.quantity_example)

            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(dp(20), dp(8), dp(20), 0)

            layout.addView(nameInput)
            layout.addView(quantityInput)

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_grocery_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.add)) { _, _ ->

                    val name = nameInput.text.toString().trim()
                    val quantity = quantityInput.text.toString().trim()

                    if (name.isNotEmpty() && quantity.isNotEmpty()) {

                        lifecycleScope.launch {
                            try {
                                UserRepository.addGroceryItem(
                                    name,
                                    quantity
                                )
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.err_add_item, e.message.orEmpty()),
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                    } else {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            getString(R.string.enter_both_fields),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // Share grocery list
        button(c, getString(R.string.share_list), 20) {
            shareList()
        }
    }



    private fun shareList() {

        val items = UserRepository.groceryItems.value

        val listText = buildString {

            append(getString(R.string.grocery_share_header) + "\n\n")

            if (items.isEmpty()) {
                append(getString(R.string.grocery_empty))
            } else {
                items.forEach { item ->

                    val status = if (item.checked) "✓" else "□"

                    append("$status ${item.name} — ${item.quantity}\n")
                }
            }
        }

        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, listText)
                },
                getString(R.string.share_chooser_title)
            )
        )
    }



    private fun profile(c: LinearLayout) { val u = currentProfile(); c.gravity = Gravity.CENTER_HORIZONTAL; text(c, "◯", 70, grey, center = true); text(c, u.displayName, 20, brown, true, center = true, top = 10); text(c, getString(R.string.profile_level_xp, u.level, u.xp), 14, terra, center = true); text(c, getString(R.string.profile_streak, u.streakCount), 16, brown, top = 18); text(c, getString(R.string.profile_stats, u.bakesCompleted, u.recipesShared), 14, grey, top = 24); outline(c, getString(R.string.settings_button), 22) { show("settings") } }

    private fun createRecipe(c: LinearLayout) {
        outline(c, getString(R.string.back_button)) { onBackPressedDispatcher.onBackPressed() }; text(c, getString(R.string.new_recipe_title), 22, brown, true, top = 12); val title = edit(c, getString(R.string.recipe_title_hint), top = 16); text(c, getString(R.string.ingredients), 16, brown, true, top = 20, bottom = 8); val ingredient = edit(c, getString(R.string.ingredient_example)); text(c, getString(R.string.steps), 16, brown, true, top = 20, bottom = 8); val step = edit(c, getString(R.string.first_step_hint)); val status = text(c, "", 13, terra, top = 8); status.visibility = View.GONE
        button(c, getString(R.string.publish_recipe), 20) { if (ingredient.text.isBlank() || step.text.isBlank()) { status.text = getString(R.string.err_recipe_incomplete); status.visibility = View.VISIBLE; return@button }; val r = Recipe("user-${UUID.randomUUID()}", title.text.toString().ifBlank { getString(R.string.untitled_recipe) }, AuthRepository.currentUser?.displayName ?: "Baker", "User", "Other", Difficulty.EASY, 0, 0, 1, 0f, 0, Nutrition(0, 0f, 0f, 0f), listOf(Ingredient(ingredient.text.toString().trim(), "", "")), listOf(RecipeStep(1, "Step 1", step.text.toString().trim()))); lifecycleScope.launch { try { RecipeRepository.addRecipe(r); show("recipe_box") } catch (e: Exception) { status.text = e.message ?: getString(R.string.err_publish); status.visibility = View.VISIBLE } } }
    }

    private fun settings(c: LinearLayout) {
        outline(c, getString(R.string.back_button)) { onBackPressedDispatcher.onBackPressed() }
        text(c, getString(R.string.settings_title), 22, brown, true, top = 12, bottom = 20)

        val units = Switch(this)
        units.text = getString(R.string.imperial_units)
        units.isChecked = currentProfile().unitPreference == "Imperial"
        units.setTextColor(brown)
        units.setOnCheckedChangeListener { _, checked ->
            lifecycleScope.launch { runCatching { UserRepository.setUnitPreference(if (checked) "Imperial" else "Metric") } }
        }
        c.addView(units)

        // Real-time notifications opt-in
        val notifications = Switch(this)
        notifications.text = getString(R.string.notifications_toggle)
        notifications.setTextColor(brown)
        notifications.isChecked = AppPreferences.notificationsEnabled
        notifications.setOnCheckedChangeListener { _, checked ->
            AppPreferences.notificationsEnabled = checked
            AppNotifications.syncSubscription()
            if (checked) requestNotificationPermissionIfNeeded()
        }
        c.addView(notifications, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        outline(c, getString(R.string.language_button, languageLabel(LocaleHelper.currentTag(this))), 16) { showLanguageDialog() }
        outline(c, getString(R.string.privacy_policy), 8) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url))))
        }
        button(c, getString(R.string.log_out), 24) {
            lifecycleScope.launch {
                AuthRepository.logout(this@MainActivity)
                history.clear()
                show("onboarding", false)
            }
        }
        outline(c, getString(R.string.delete_account), 8) { confirmDeleteAccount() }
    }

    // ------------------------------------------------------------------ language

    private fun languageLabel(tag: String) = when (tag) {
        "en" -> getString(R.string.lang_english)
        "es" -> getString(R.string.lang_spanish)
        "fr" -> getString(R.string.lang_french)
        "ar" -> getString(R.string.lang_arabic)
        else -> getString(R.string.language_system)
    }

    private fun showLanguageDialog() {
        val tags = listOf("") + LocaleHelper.SUPPORTED
        val labels = tags.map { languageLabel(it) }.toTypedArray()
        val current = tags.indexOf(LocaleHelper.currentTag(this)).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.language_title))
            .setSingleChoiceItems(labels, current) { dialog, which ->
                dialog.dismiss()
                LocaleHelper.setLanguage(this, tags[which])
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ------------------------------------------------------------------ Google SSO

    /** Web client ID generated by the google-services plugin once Google sign-in is enabled. */
    private fun googleWebClientId(): String? {
        val id = resources.getIdentifier("default_web_client_id", "string", packageName)
        return if (id != 0) getString(id).takeIf { it.isNotBlank() } else null
    }

    private fun signInWithGoogle(status: TextView) {
        fun showError(message: String) { status.text = message; status.visibility = View.VISIBLE }
        val clientId = googleWebClientId()
        if (clientId == null) { showError(getString(R.string.google_not_configured)); return }
        if (!NetworkMonitor.isOnline.value) { showError(getString(R.string.err_needs_internet)); return }

        lifecycleScope.launch {
            try {
                AuthRepository.signInWithGoogle(this@MainActivity, clientId)
                activateFirebase()
                history.clear()
                show("home", false)
            } catch (e: GetCredentialCancellationException) {
                // The user dismissed the account picker - nothing to report.
            } catch (e: NoCredentialException) {
                showError(getString(R.string.google_no_account))
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.google_failed))
            }
        }
    }

    // ------------------------------------------------------------------ delete account / report

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_account_title))
            .setMessage(getString(R.string.delete_account_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> startAccountDeletion() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun startAccountDeletion() {
        if (!NetworkMonitor.isOnline.value) {
            Toast.makeText(this, getString(R.string.err_needs_internet), Toast.LENGTH_LONG).show()
            return
        }
        val user = AuthRepository.currentUser ?: return
        if (user.providerData.any { it.providerId == "password" }) {
            val input = EditText(this)
            input.hint = getString(R.string.password_hint)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.reauth_title))
                .setView(input)
                .setPositiveButton(getString(R.string.delete)) { _, _ -> runAccountDeletion(input.text.toString()) }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            runAccountDeletion(null)
        }
    }

    private fun runAccountDeletion(password: String?) {
        lifecycleScope.launch {
            try {
                AuthRepository.deleteAccount(this@MainActivity, password, googleWebClientId())
                history.clear()
                show("onboarding", false)
                Toast.makeText(this@MainActivity, getString(R.string.account_deleted), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.err_delete_account, e.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** User-generated-content reporting (Google Play policy). */
    private fun showReportDialog(r: Recipe) {
        val reasonKeys = listOf("offensive", "spam", "copyright", "other")
        val labels = arrayOf(
            getString(R.string.report_reason_offensive),
            getString(R.string.report_reason_spam),
            getString(R.string.report_reason_copyright),
            getString(R.string.report_reason_other)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.report_dialog_title))
            .setItems(labels) { _, which ->
                lifecycleScope.launch {
                    try {
                        RecipeRepository.reportRecipe(r.id, reasonKeys[which])
                        Toast.makeText(this@MainActivity, getString(R.string.report_sent), Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, getString(R.string.report_failed), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }



    private fun recipeDetail(c: LinearLayout) {

        val r = selectedRecipe ?: run { show("home", false); return }

        outline(c, getString(R.string.back_button)) {
            onBackPressedDispatcher.onBackPressed()
        }

        // RECIPE INFORMATION
        text(
            c,
            getString(R.string.detail_info_full, r.category, r.source, r.avgRating.toString(), r.totalTimeMinutes, r.servings),
            14,
            grey,
            bottom = 20
        )

        // LARGE RECIPE IMAGE
        val recipeImage = ImageView(this)

        val imageParams = LinearLayout.LayoutParams(
            -1,
            dp(220)
        )

        imageParams.setMargins(0, 0, 0, dp(16))

        recipeImage.layoutParams = imageParams
        recipeImage.scaleType = ImageView.ScaleType.CENTER_CROP
        recipeImage.background = getDrawable(R.drawable.bg_card)
        recipeImage.clipToOutline = true

        Glide.with(this)
            .load(r.imageUrl)
            .placeholder(android.R.color.darker_gray)
            .error(android.R.color.darker_gray)
            .centerCrop()
            .into(recipeImage)

        c.addView(recipeImage)

        // INGREDIENTS SECTION
        text(
            c,
            getString(R.string.ingredients),
            20,
            brown,
            true,
            bottom = 8
        )

        if (r.ingredients.isEmpty()) {

            text(
                c,
                getString(R.string.no_ingredients),
                14,
                grey,
                bottom = 16
            )

        } else {

            r.ingredients.forEachIndexed { index, ingredient ->

                val measurement = listOf(
                    ingredient.quantity.trim(),
                    ingredient.unit.trim()
                ).filter { it.isNotBlank() }
                    .joinToString(" ")

                val ingredientText = if (measurement.isNotBlank()) {
                    "${index + 1}. $measurement ${ingredient.name}"
                } else {
                    "${index + 1}. ${ingredient.name}"
                }

                text(
                    c,
                    ingredientText,
                    15,
                    brown,
                    top = 5,
                    bottom = 5
                )
            }
        }

        // RECIPE INSTRUCTIONS
        text(
            c,
            getString(R.string.instructions),
            20,
            brown,
            true,
            top = 24,
            bottom = 8
        )

        r.steps.forEachIndexed { index, step ->

            text(c, getString(R.string.step_number, step.stepNumber), 16, brown, true)
            text(c, step.instruction, 14, grey, top = 4)
        }

        // BAKE MODE
        button(c, getString(R.string.start_bake), 20) {
            show("bake_mode")
        }

        // SAVE RECIPE
        outline(
            c,
            if (UserRepository.savedRecipeIds.value.contains(r.id))
                getString(R.string.saved_to_box)
            else
                getString(R.string.save_to_box),
            10
        ) {
            lifecycleScope.launch {
                try {
                    UserRepository.toggleSaved(r.id)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, e.message ?: getString(R.string.err_update_item, ""), Toast.LENGTH_LONG).show()
                }
                if (route == "recipe_detail") show("recipe_detail", false)
            }
        }

        // REPORT (community-published recipes only)
        if (r.source == "User") {
            outline(c, getString(R.string.report_recipe), 10) { showReportDialog(r) }
        }

        // ORIGINAL RECIPE SOURCE
        if (r.sourceUrl != null) {
            outline(c, getString(R.string.view_original)) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(r.sourceUrl)
                    )
                )
            }
        }
    }


    private fun bakeMode(c: LinearLayout) {

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val r = selectedRecipe ?: run { show("home", false); return }
        var index = 0

        if (r.steps.isEmpty()) {
            text(c, getString(R.string.no_steps), 15, grey, center = true, top = 24)
            outline(c, getString(R.string.bake_exit), 16) { onBackPressedDispatcher.onBackPressed() }
            return
        }

        // Timer variables
        var countDownTimer: android.os.CountDownTimer? = null
        var remainingSeconds = 0
        var isTimerRunning = false

        val title = text(
            c, "", 24, brown, true,
            center = true, top = 24
        )

        val instruction = text(
            c, "", 15, grey,
            center = true, top = 12, bottom = 22
        )

        val timer = text(
            c, "", 36, terra, true,
            center = true, bottom = 12
        )

        // Timer control buttons
        val timerControls = LinearLayout(this)
        timerControls.orientation = LinearLayout.HORIZONTAL
        timerControls.gravity = Gravity.CENTER

        c.addView(
            timerControls,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(24)
            }
        )

        val startPause = button(timerControls, getString(R.string.bake_start))
        val reset = outline(timerControls, getString(R.string.bake_reset))

        startPause.layoutParams = LinearLayout.LayoutParams(
            0, dp(48), 1f
        ).apply {
            marginEnd = dp(5)
        }

        reset.layoutParams = LinearLayout.LayoutParams(
            0, dp(48), 1f
        ).apply {
            marginStart = dp(5)
        }

        // Navigation buttons
        val nav = LinearLayout(this)
        nav.orientation = LinearLayout.HORIZONTAL

        c.addView(
            nav,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(12)
            }
        )

        val back = outline(nav, getString(R.string.bake_back))
        val exit = outline(nav, getString(R.string.bake_exit))
        val next = button(nav, getString(R.string.bake_next))

        back.layoutParams = LinearLayout.LayoutParams(
            0, dp(52), 1f
        ).apply {
            marginEnd = dp(4)
        }

        exit.layoutParams = LinearLayout.LayoutParams(
            0, dp(52), 1f
        ).apply {
            marginStart = dp(4)
            marginEnd = dp(4)
        }

        next.layoutParams = LinearLayout.LayoutParams(
            0, dp(52), 1f
        ).apply {
            marginStart = dp(4)
        }

        // Called whenever Bake Mode is left (exit, system back, tab change, activity destroyed).
        bakeCleanup = {
            countDownTimer?.cancel()
            countDownTimer = null
            isTimerRunning = false
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Display the countdown
        fun updateTimerDisplay() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60

            timer.text = "%02d:%02d".format(minutes, seconds)
        }

        // Stop and reset the timer for the current step

        fun resetStepTimer() {

            countDownTimer?.cancel()
            countDownTimer = null
            isTimerRunning = false

            val seconds = r.steps[index].timerSeconds

            if (seconds != null && seconds > 0) {

                remainingSeconds = seconds

                timer.visibility = View.VISIBLE
                timerControls.visibility = View.VISIBLE

                updateTimerDisplay()

                startPause.text = getString(R.string.bake_start)
                startPause.isEnabled = true
                reset.isEnabled = true

            } else {

                remainingSeconds = 0

                // Hide the timer completely when no duration exists.
                timer.visibility = View.GONE
                timerControls.visibility = View.GONE

            }
        }


        // Start or resume the countdown
        fun startTimer() {

            if (remainingSeconds <= 0 || isTimerRunning) return

            countDownTimer?.cancel()
            isTimerRunning = true

            startPause.text = getString(R.string.bake_pause)

            countDownTimer = object : android.os.CountDownTimer(
                remainingSeconds * 1000L,
                1000L
            ) {

                override fun onTick(millisUntilFinished: Long) {

                    remainingSeconds =
                        ((millisUntilFinished + 999) / 1000).toInt()

                    updateTimerDisplay()
                }

                override fun onFinish() {

                    remainingSeconds = 0
                    isTimerRunning = false

                    timer.text = "00:00"
                    startPause.text = getString(R.string.bake_start)

                    countDownTimer = null

                    android.widget.Toast.makeText(
                        this@MainActivity,
                        getString(R.string.timer_finished),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    NotificationHelper.showTimerDone(applicationContext, r.title)
                }
            }.start()
        }

        // Render the current baking step
        fun render() {

            val s = r.steps[index]

            title.text = getString(R.string.bake_step_of, index + 1, r.steps.size)

            instruction.text = s.instruction

            resetStepTimer()

            back.isEnabled = index > 0
            back.alpha = if (index > 0) 1f else 0.5f

            next.text = if (index < r.steps.lastIndex) {
                getString(R.string.bake_next)
            } else {
                getString(R.string.bake_finish)
            }
        }

        // Start / Pause button
        startPause.setOnClickListener {

            if (isTimerRunning) {

                countDownTimer?.cancel()
                countDownTimer = null
                isTimerRunning = false

                startPause.text = getString(R.string.bake_resume)

            } else {

                startTimer()
            }
        }

        // Reset button
        reset.setOnClickListener {
            resetStepTimer()
        }

        // Back button
        back.setOnClickListener {

            if (index > 0) {
                index++
                index -= 2
                render()
            }
        }

        // Exit Bake Mode
        exit.setOnClickListener {

            countDownTimer?.cancel()

            window.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )

            onBackPressedDispatcher.onBackPressed()
        }

        // Next / Finish button
        next.setOnClickListener {

            countDownTimer?.cancel()
            isTimerRunning = false

            if (index < r.steps.lastIndex) {

                index++
                render()

            } else {

                // Go back to the recipe instead of stacking another entry on the history.
                if (history.isNotEmpty()) show(history.removeLast(), false) else show("recipe_detail", false)
            }
        }

        render()
    }


    private fun recipeCard(parent: LinearLayout, r: Recipe) = recipeRow(parent, r)

    private fun recipeRow(parent: LinearLayout, r: Recipe, label: String? = null) {

        // Main recipe card
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.setBackgroundResource(R.drawable.bg_card)
        card.setPadding(dp(10), dp(10), dp(10), dp(10))

        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, 0, 0, dp(10))
        parent.addView(card, lp)

        // Recipe image
        val image = ImageView(this)
        image.layoutParams = LinearLayout.LayoutParams(
            dp(105),
            dp(105)
        ).apply {
            marginEnd = dp(12)
        }

        image.scaleType = ImageView.ScaleType.CENTER_CROP

        // Load image from TheMealDB using Glide
        Glide.with(this)
            .load(r.imageUrl)
            .placeholder(android.R.color.darker_gray)
            .error(android.R.color.darker_gray)
            .centerCrop()
            .into(image)

        card.addView(image)

        // Recipe information
        val infoLayout = LinearLayout(this)
        infoLayout.orientation = LinearLayout.VERTICAL

        infoLayout.layoutParams = LinearLayout.LayoutParams(
            0,
            -2,
            1f
        )

        card.addView(infoLayout)

        text(infoLayout, r.title, 16, brown, true)

        text(
            infoLayout,
            getString(R.string.row_info, r.source, r.avgRating.toString(), r.totalTimeMinutes),
            14,
            grey,
            top = 4
        )

        label?.let {
            text(infoLayout, it, 13, grey, top = 3)
        }

        // Open recipe details when clicked
        card.setOnClickListener {
            selectedRecipe = r
            show("recipe_detail")
        }
    }

    private fun text(p: LinearLayout, value: String, size: Int, color: Int, bold: Boolean = false, center: Boolean = false, top: Int = 0, bottom: Int = 0): TextView { val v = TextView(this); v.text = value; v.textSize = size.toFloat(); v.setTextColor(color); if (bold) v.setTypeface(null, Typeface.BOLD); if (center) v.gravity = Gravity.CENTER; v.setPadding(0, dp(top), 0, dp(bottom)); p.addView(v); return v }
    private fun edit(p: LinearLayout, hint: String, password: Boolean = false, top: Int = 0): EditText { val e = EditText(this); e.hint = hint; e.setTextColor(brown); e.setHintTextColor(grey); e.setSingleLine(true); if (password) e.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(top), 0, 0); p.addView(e, lp); return e }
    private fun button(p: LinearLayout, label: String, top: Int = 0, action: () -> Unit = {}): Button { val b = Button(this); b.text = label; b.setTextColor(white); b.setBackgroundResource(R.drawable.bg_terracotta); b.setOnClickListener { action() }; val lp = LinearLayout.LayoutParams(-1, dp(52)); lp.setMargins(0, dp(top), 0, 0); p.addView(b, lp); return b }
    private fun outline(p: LinearLayout, label: String, top: Int = 0, action: () -> Unit = {}): Button { val b = Button(this); b.text = label; b.setTextColor(brown); b.setOnClickListener { action() }; val lp = LinearLayout.LayoutParams(-1, dp(52)); lp.setMargins(0, dp(top), 0, 0); p.addView(b, lp); return b }
    private fun spacer(p: LinearLayout, h: Int) { p.addView(Space(this), LinearLayout.LayoutParams(1, dp(h))) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
private class SimpleWatcher(private val change: () -> Unit) : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit; override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = change(); override fun afterTextChanged(e: android.text.Editable?) = Unit }
