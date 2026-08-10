import test from "node:test";
import assert from "node:assert/strict";
import { validateListing } from "../src/validation.js";

test("accepts a trade listing with a storage image path", () => {
  assert.deepEqual(validateListing({
    title: "Raleigh bike",
    description: "Fresh tune-up",
    offerType: "trade",
    imagePaths: ["listingImages/user-1/listing-1/photo.jpg"],
  }), []);
});

test("rejects unrecognized offer types and missing images", () => {
  const errors = validateListing({ title: "x", description: "y", offerType: "free", imagePaths: [] });
  assert(errors.includes("offerType"));
  assert(errors.includes("imagePaths"));
});
