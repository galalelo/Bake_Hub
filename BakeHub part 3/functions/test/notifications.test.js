'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { buildNewRecipeMessage, NEW_RECIPES_TOPIC } = require('../lib/notifications');

test('builds a data-only topic message for a community recipe', () => {
  const msg = buildNewRecipeMessage('abc123', { source: 'User', title: 'Lemon Drizzle', authorName: 'Sam' });
  assert.equal(msg.topic, NEW_RECIPES_TOPIC);
  assert.deepEqual(msg.data, { type: 'new_recipe', recipeId: 'abc123', title: 'Lemon Drizzle', author: 'Sam' });
  assert.equal(msg.notification, undefined, 'must be data-only so the app can localise the text');
  assert.equal(msg.android.priority, 'high');
});

test('does not notify for seeded sample recipes or external recipes', () => {
  assert.equal(buildNewRecipeMessage('r1', { source: 'BakeHub', title: 'Banana Bread' }), null);
  assert.equal(buildNewRecipeMessage('r2', { source: 'TheMealDB', title: 'Apple Pie' }), null);
});

test('ignores documents with no usable title or id', () => {
  assert.equal(buildNewRecipeMessage('x', { source: 'User', title: '   ' }), null);
  assert.equal(buildNewRecipeMessage('', { source: 'User', title: 'Cake' }), null);
  assert.equal(buildNewRecipeMessage('x', undefined), null);
});

test('truncates very long titles and author names', () => {
  const msg = buildNewRecipeMessage('x', { source: 'User', title: 'a'.repeat(500), authorName: 'b'.repeat(500) });
  assert.equal(msg.data.title.length, 100);
  assert.equal(msg.data.author.length, 60);
});

test('missing author becomes an empty string', () => {
  assert.equal(buildNewRecipeMessage('x', { source: 'User', title: 'Cake' }).data.author, '');
});
