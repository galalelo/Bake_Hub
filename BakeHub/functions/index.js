const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { defineSecret } = require('firebase-functions/params');
const logger = require('firebase-functions/logger');

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
