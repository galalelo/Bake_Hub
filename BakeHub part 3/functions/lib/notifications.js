'use strict';

/** FCM topic every opted-in device subscribes to (see AppNotifications.kt). */
const NEW_RECIPES_TOPIC = 'new_recipes';

/**
 * Builds the data-only FCM message announcing a newly published community recipe.
 * Returns null when no notification should be sent (seed/sample data, external recipes,
 * or documents missing the fields the app needs).
 *
 * The message is data-only on purpose: the Android app builds the visible notification
 * itself so the text appears in the user's chosen app language.
 */
function buildNewRecipeMessage(recipeId, recipe) {
  if (!recipeId || !recipe) return null;
  if (recipe.source !== 'User') return null; // only community-published recipes
  const title = String(recipe.title || '').trim();
  if (!title) return null;

  return {
    topic: NEW_RECIPES_TOPIC,
    data: {
      type: 'new_recipe',
      recipeId: String(recipeId),
      title: title.slice(0, 100),
      author: String(recipe.authorName || '').slice(0, 60),
    },
    android: { priority: 'high', ttl: 60 * 60 * 1000 }, // stale after an hour
  };
}

module.exports = { NEW_RECIPES_TOPIC, buildNewRecipeMessage };
