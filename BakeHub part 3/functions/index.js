const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { defineSecret } = require('firebase-functions/params');
const logger = require('firebase-functions/logger');
const { initializeApp } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');
const { buildNewRecipeMessage } = require('./lib/notifications');

initializeApp();

const spoonacularKey = defineSecret('SPOONACULAR_API_KEY');

exports.searchExternalRecipes = onCall({ secrets: [spoonacularKey], region: 'europe-west1' }, async (request) => {
  const query = String(request.data?.query || '').trim();
  if (!query) return { recipes: [] };
  if (query.length > 80) throw new HttpsError('invalid-argument', 'Search query is too long.');

  const url = new URL('https://api.spoonacular.com/recipes/complexSearch');
  url.searchParams.set('query', query);
  url.searchParams.set('number', '10');
  url.searchParams.set('addRecipeInformation', 'true');
  url.searchParams.set('apiKey', spoonacularKey.value());

  const response = await fetch(url);
  if (!response.ok) {
    logger.error('Recipe provider failed', { status: response.status });
    throw new HttpsError('unavailable', 'The online recipe service is temporarily unavailable.');
  }
  const payload = await response.json();
  return { recipes: (payload.results || []).map((r) => ({
    id: `external-${r.id}`,
    title: r.title || 'Untitled recipe',
    authorName: 'Spoonacular',
    source: 'External',
    sourceUrl: r.sourceUrl || r.spoonacularSourceUrl || null,
    imageUrl: r.image || null,
    category: (r.dishTypes || [])[0] || 'Other',
    servings: r.servings || 1,
    ingredients: (r.extendedIngredients || []).map((i) => ({ name: i.name || '', quantity: String(i.amount || ''), unit: i.unit || '' })),
    instructions: r.instructions || 'Open the original recipe for full instructions.'
  })) };
});

/**
 * Real-time notifications: whenever a community member publishes a recipe, push a data
 * message to every device subscribed to the "new_recipes" topic.
 * NOTE: `region` must match your Firestore database location (change it if yours differs).
 */
exports.notifyNewRecipe = onDocumentCreated(
  { document: 'recipes/{recipeId}', region: 'europe-west1' },
  async (event) => {
    const message = buildNewRecipeMessage(event.params.recipeId, event.data?.data());
    if (!message) return;
    try {
      await getMessaging().send(message);
    } catch (err) {
      logger.error('Could not send new-recipe notification', { error: String(err) });
    }
  }
);
