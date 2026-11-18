const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("dictionary").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transcriptions").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("dictionary").doc("w1").set({
      userId: ALICE_UID,
      word: "ሰላም",
      frequency: 1,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile settings: create, partial update, reject ghost fields and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Nuh User",
      inputMode: "FIDEL_INPLACE",
      foldLayoutPreference: "AUTO",
      splitGapDp: 86,
      keyHeightDp: 48,
      letterSizeSp: 19,
      themeStyle: "OBSIDIAN_GOLD",
      autoCorrectEnabled: true,
      nextFidelHintsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update allowed fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      keyHeightDp: 54,
      letterSizeSp: 21,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject out-of-range keyHeightDp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      keyHeightDp: 120,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Bob cannot read or write Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Personal Amharic dictionary: Kotlin-aligned CRUD, frequency increment, bounds, and isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // List empty subcollection succeeds for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dictionary").get()
  );

  // Create without optional note
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dictionary").doc("word_1").set({
      userId: ALICE_UID,
      word: "ኢትዮጵያ",
      frequency: 5,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create with optional note
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dictionary").doc("word_2").set({
      userId: ALICE_UID,
      word: "እንኳን",
      note: "Welcome greeting",
      frequency: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Frequency bump partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dictionary").doc("word_1").update({
      frequency: 6,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("dictionary").doc("word_future").set({
      userId: ALICE_UID,
      word: "ነገ",
      frequency: 1,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject unexpected `id` field inside document body
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("dictionary").doc("word_id_bad").set({
      id: "word_id_bad",
      userId: ALICE_UID,
      word: "ሰላም",
      frequency: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob cannot list, read, write, or delete Alice's dictionary
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dictionary").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dictionary").doc("word_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dictionary").doc("word_1").delete());

  // Alice can delete her entry
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dictionary").doc("word_2").delete()
  );
});

test("Voice transcriptions: Kotlin-aligned CRUD, bounds, and isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 4000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transcriptions").doc("tr_1").set({
      userId: ALICE_UID,
      transcript: "ሰላም እንደምን ዋልክ",
      modelName: "gemini-3.5-transcribe",
      durationSeconds: 4,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Ordered query matching repository
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transcriptions")
      .orderBy("createdAt", "desc")
      .get()
  );

  // Reject blank transcript
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transcriptions").doc("tr_blank").set({
      userId: ALICE_UID,
      transcript: "",
      modelName: "gemini-3.5-transcribe",
      durationSeconds: 2,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transcriptions").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transcriptions").doc("tr_1").delete()
  );
});
