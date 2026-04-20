const admin = require("firebase-admin");
const fs = require("fs");
const path = require("path");

const keyPath = path.join(__dirname, "serviceAccountKey.json");
const dataPath = path.join(__dirname, "seed-data.json");
const reviewsPath = path.join(__dirname, "seed-reviews.json");

const serviceAccount = require(keyPath);
const seedData = JSON.parse(fs.readFileSync(dataPath, "utf8"));
const seedReviews = JSON.parse(fs.readFileSync(reviewsPath, "utf8"));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();

async function upsertCollection(collectionName, items) {
  if (!Array.isArray(items) || items.length === 0) return;
  const batch = db.batch();

  items.forEach((item) => {
    const id = item.id || db.collection(collectionName).doc().id;
    const ref = db.collection(collectionName).doc(id);
    batch.set(ref, { ...item, id }, { merge: true });
  });

  await batch.commit();
  console.log(`Seeded ${items.length} docs -> ${collectionName}`);
}

async function run() {
  try {
    await upsertCollection("categories", seedData.categories);
    await upsertCollection("products", seedData.products);
    await upsertCollection("reviews", seedReviews.reviews);
    await upsertCollection("discount_codes", seedReviews.discount_codes);
    console.log("Seed completed.");
    process.exit(0);
  } catch (e) {
    console.error("Seed failed:", e);
    process.exit(1);
  }
}

run();