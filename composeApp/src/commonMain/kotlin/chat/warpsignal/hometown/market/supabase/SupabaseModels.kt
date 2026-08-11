package chat.warpsignal.hometown.market.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Public Supabase configuration. The publishable key is intentionally injected by each deploy target. */
data class SupabaseConfig(
    val baseUrl: String = "https://trades.dishman.xyz",
    val publishableKey: String,
)

@Serializable
data class SupabaseListing(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val title: String,
    val description: String,
    @SerialName("offer_type") val offerType: String,
    @SerialName("price_cents") val priceCents: Int?,
    val neighborhood: String,
    val status: String,
)

@Serializable
data class CreateListingRequest(
    @SerialName("owner_id") val ownerId: String,
    val title: String,
    val description: String,
    @SerialName("offer_type") val offerType: String,
    @SerialName("price_cents") val priceCents: Int?,
    val neighborhood: String,
)

interface MarketplaceRepository {
    /** Public: no user token required; RLS returns only active listings. */
    suspend fun publicListings(): List<SupabaseListing>
    /** Authenticated: sends a listing whose owner_id must match the current Supabase user. */
    suspend fun createListing(listing: CreateListingRequest, accessToken: String)
    /** Authenticated: writes a comment subject to Supabase RLS. */
    suspend fun addComment(listingId: String, body: String, accessToken: String)
    /** Authenticated upload location, constrained by the Storage RLS policy. */
    fun listingImagePath(userId: String, listingId: String, fileName: String): String =
        "$userId/$listingId/$fileName"
}
