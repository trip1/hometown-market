import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { validateListing } from "./validation.js";

initializeApp();
const db = getFirestore();

function requireUser(request) {
  if (!request.auth) throw new HttpsError("unauthenticated", "Sign in is required.");
  return request.auth.uid;
}

export const createListing = onCall({ region: "us-central1" }, async (request) => {
  const uid = requireUser(request);
  const input = request.data;
  const errors = validateListing(input);
  if (errors.length) throw new HttpsError("invalid-argument", `Invalid: ${errors.join(", ")}`);
  const listing = {
    title: input.title.trim(), description: input.description.trim(), offerType: input.offerType,
    imagePaths: input.imagePaths, neighborhood: String(input.neighborhood || "").trim().slice(0, 120),
    priceCents: Number.isInteger(input.priceCents) && input.priceCents >= 0 ? input.priceCents : null,
    ownerId: uid, status: "active", createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(),
  };
  const ref = await db.collection("listings").add(listing);
  return { id: ref.id };
});

export const addComment = onCall({ region: "us-central1" }, async (request) => {
  const uid = requireUser(request);
  const { listingId, body } = request.data || {};
  if (typeof listingId !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(listingId) || typeof body !== "string" || body.trim().length < 1 || body.length > 1000) {
    throw new HttpsError("invalid-argument", "A listing ID and a 1–1000 character comment are required.");
  }
  const listing = await db.doc(`listings/${listingId}`).get();
  if (!listing.exists || listing.get("status") !== "active") throw new HttpsError("not-found", "Listing not found.");
  const ref = await db.collection(`listings/${listingId}/comments`).add({ authorId: uid, body: body.trim(), createdAt: FieldValue.serverTimestamp() });
  return { id: ref.id };
});
