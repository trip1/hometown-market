const OFFER_TYPES = new Set(["cash", "cash_only", "trade", "trade_only"]);

export function validateListing(input) {
  const errors = [];
  if (typeof input?.title !== "string" || input.title.trim().length < 3 || input.title.length > 120) errors.push("title");
  if (typeof input?.description !== "string" || input.description.trim().length < 10 || input.description.length > 4000) errors.push("description");
  if (!OFFER_TYPES.has(input?.offerType)) errors.push("offerType");
  if (!Array.isArray(input?.imagePaths) || input.imagePaths.length < 1 || input.imagePaths.length > 8 || !input.imagePaths.every((path) => typeof path === "string" && path.startsWith("listingImages/"))) errors.push("imagePaths");
  return errors;
}
