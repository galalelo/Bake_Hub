import { readFile } from "node:fs/promises";
import { after, before, beforeEach, describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { addDoc, collection, deleteDoc, doc, getDoc, serverTimestamp, setDoc, updateDoc } from "firebase/firestore";

const projectId = "bakehub-rules-test";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    firestore: {
      rules: await readFile(new URL("./firestore.rules", import.meta.url), "utf8"),
    },
  });
});

after(async () => {
  await testEnv?.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users/alice"), { displayName: "Alice" });
    await setDoc(doc(context.firestore(), "users/alice/groceryItems/flour"), {
      name: "Flour",
      quantity: "2 cups",
      checked: false,
    });
  });
});

describe("Firestore groceryItems rules", () => {
  it("allows an authenticated user to read and manage their own grocery items", async () => {
    const db = testEnv.authenticatedContext("alice").firestore();
    const item = doc(db, "users/alice/groceryItems/flour");

    await assertSucceeds(getDoc(item));
    await assertSucceeds(setDoc(doc(db, "users/alice/groceryItems/sugar"), {
      name: "Sugar",
      quantity: "1 cup",
      checked: false,
    }));
    await assertSucceeds(updateDoc(item, { checked: true }));
    await assertSucceeds(deleteDoc(item));
  });

  it("denies access to another user's grocery items", async () => {
    const db = testEnv.authenticatedContext("bob").firestore();
    const item = doc(db, "users/alice/groceryItems/flour");

    await assertFails(getDoc(item));
    await assertFails(setDoc(doc(db, "users/alice/groceryItems/unauthorized"), {
      name: "Unauthorized",
      quantity: "1",
    }));
    await assertFails(updateDoc(item, { checked: true }));
    await assertFails(deleteDoc(item));
  });

  it("denies unauthenticated access to grocery items", async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    const item = doc(db, "users/alice/groceryItems/flour");

    await assertFails(getDoc(item));
    await assertFails(setDoc(doc(db, "users/alice/groceryItems/anonymous"), {
      name: "Anonymous",
      quantity: "1",
    }));
  });

  it("continues to allow users to read and write only their own profile", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    const bobDb = testEnv.authenticatedContext("bob").firestore();

    await assertSucceeds(getDoc(doc(aliceDb, "users/alice")));
    await assertSucceeds(updateDoc(doc(aliceDb, "users/alice"), { displayName: "Alice A." }));
    await assertFails(getDoc(doc(bobDb, "users/alice")));
    await assertFails(updateDoc(doc(bobDb, "users/alice"), { displayName: "Bob" }));
  });
});

describe("Firestore recipes rules", () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "recipes/r1"), {
        title: "Banana Bread",
        authorName: "Alice",
        source: "User",
      });
    });
  });

  it("lets signed-in users read recipes but not anonymous visitors", async () => {
    await assertSucceeds(getDoc(doc(testEnv.authenticatedContext("bob").firestore(), "recipes/r1")));
    await assertFails(getDoc(doc(testEnv.unauthenticatedContext().firestore(), "recipes/r1")));
  });

  it("lets signed-in users publish recipes but not anonymous visitors", async () => {
    const recipe = { title: "Scones", authorName: "Bob", source: "User" };
    await assertSucceeds(setDoc(doc(testEnv.authenticatedContext("bob").firestore(), "recipes/r2"), recipe));
    await assertFails(setDoc(doc(testEnv.unauthenticatedContext().firestore(), "recipes/r3"), recipe));
  });

  it("only lets the author (matched by display name) edit or delete a recipe", async () => {
    const author = testEnv.authenticatedContext("alice", { name: "Alice" }).firestore();
    const other = testEnv.authenticatedContext("bob", { name: "Bob" }).firestore();
    await assertFails(updateDoc(doc(other, "recipes/r1"), { title: "Hacked" }));
    await assertFails(deleteDoc(doc(other, "recipes/r1")));
    await assertSucceeds(updateDoc(doc(author, "recipes/r1"), { title: "Banana Bread v2" }));
    await assertSucceeds(deleteDoc(doc(author, "recipes/r1")));
  });
});

describe("Firestore reports rules (user-generated-content reporting)", () => {
  const validReport = (uid) => ({
    recipeId: "r1",
    reporterUid: uid,
    reason: "spam",
    createdAt: serverTimestamp(),
  });

  it("lets a signed-in user file a report as themselves", async () => {
    const db = testEnv.authenticatedContext("bob").firestore();
    await assertSucceeds(addDoc(collection(db, "reports"), validReport("bob")));
  });

  it("rejects reports filed on behalf of another user", async () => {
    const db = testEnv.authenticatedContext("bob").firestore();
    await assertFails(addDoc(collection(db, "reports"), validReport("alice")));
  });

  it("rejects anonymous reports and reports with unexpected fields", async () => {
    await assertFails(addDoc(collection(testEnv.unauthenticatedContext().firestore(), "reports"), validReport("bob")));
    const db = testEnv.authenticatedContext("bob").firestore();
    await assertFails(addDoc(collection(db, "reports"), { ...validReport("bob"), extra: "nope" }));
  });

  it("rejects oversized reasons", async () => {
    const db = testEnv.authenticatedContext("bob").firestore();
    await assertFails(addDoc(collection(db, "reports"), { ...validReport("bob"), reason: "x".repeat(201) }));
  });

  it("does not let clients read, edit or delete reports", async () => {
    let reportPath;
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const ref = await addDoc(collection(context.firestore(), "reports"), validReport("bob"));
      reportPath = ref.path;
    });
    const db = testEnv.authenticatedContext("bob").firestore();
    await assertFails(getDoc(doc(db, reportPath)));
    await assertFails(updateDoc(doc(db, reportPath), { reason: "changed" }));
    await assertFails(deleteDoc(doc(db, reportPath)));
  });
});

describe("Firestore user profile (notification token)", () => {
  it("lets a user store their own FCM token but not someone else's", async () => {
    const alice = testEnv.authenticatedContext("alice").firestore();
    const bob = testEnv.authenticatedContext("bob").firestore();
    await assertSucceeds(setDoc(doc(alice, "users/alice"), { fcmToken: "t1" }, { merge: true }));
    await assertFails(setDoc(doc(bob, "users/alice"), { fcmToken: "evil" }, { merge: true }));
  });
});
