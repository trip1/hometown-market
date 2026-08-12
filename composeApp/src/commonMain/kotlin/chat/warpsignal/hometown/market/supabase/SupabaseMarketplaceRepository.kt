package chat.warpsignal.hometown.market.supabase

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.contentType
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ListingWire(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val title: String,
    val description: String,
    @SerialName("offer_type") val offerType: String,
    @SerialName("price_cents") val priceCents: Int? = null,
    val neighborhood: String,
    val status: String,
)

@Serializable
private data class ListingImageWire(
    val id: String,
    @SerialName("object_path") val objectPath: String,
    @SerialName("sort_order") val sortOrder: Int,
)

@Serializable
private data class CommentWire(
    val id: String,
    @SerialName("listing_id") val listingId: String,
    @SerialName("author_id") val authorId: String,
    val body: String,
)

class SupabaseMarketplaceRepository(
    private val config: SupabaseConfig,
    private val client: HttpClient = platformHttpClient().config {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    },
) : MarketplaceRepository {
    private fun io.ktor.client.request.HttpRequestBuilder.publicHeaders() {
        header("apikey", config.publishableKey)
    }
    private fun io.ktor.client.request.HttpRequestBuilder.authHeaders(token: String) {
        publicHeaders(); header(HttpHeaders.Authorization, "Bearer $token")
    }

    override suspend fun publicListings(): List<SupabaseListing> = client.get("${config.baseUrl}/rest/v1/listings?status=eq.active&order=created_at.desc") {
        publicHeaders()
    }.body<List<ListingWire>>().map { it.toModel() }

    override suspend fun listingImages(listingId: String): List<ListingImage> = client.get("${config.baseUrl}/rest/v1/listing_images?listing_id=eq.$listingId&order=sort_order.asc") {
        publicHeaders()
    }.body<List<ListingImageWire>>().map { ListingImage(it.id, it.objectPath, it.sortOrder) }

    override suspend fun listingComments(listingId: String): List<ListingComment> = client.get("${config.baseUrl}/rest/v1/comments?listing_id=eq.$listingId&order=created_at.asc") {
        publicHeaders()
    }.body<List<CommentWire>>().map { ListingComment(it.id, it.listingId, it.authorId, it.body) }

    override fun publicImageUrl(objectPath: String): String = "${config.baseUrl}/storage/v1/object/public/listing-images/$objectPath"

    override suspend fun addListingImage(listingId: String, ownerId: String, objectPath: String, accessToken: String) {
        client.post("${config.baseUrl}/rest/v1/listing_images") {
            authHeaders(accessToken)
            contentType(ContentType.Application.Json)
            setBody(mapOf("listing_id" to listingId, "owner_id" to ownerId, "object_path" to objectPath))
        }
    }

    override suspend fun createListing(listing: CreateListingRequest, accessToken: String): SupabaseListing {
        val created = client.post("${config.baseUrl}/rest/v1/listings") {
            authHeaders(accessToken)
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(listing)
        }.body<List<ListingWire>>().single()
        return created.toModel()
    }

    override suspend fun addComment(listingId: String, body: String, accessToken: String) {
        client.post("${config.baseUrl}/rest/v1/comments") {
            authHeaders(accessToken)
            contentType(ContentType.Application.Json)
            setBody(mapOf("listing_id" to listingId, "body" to body))
        }
    }

    override suspend fun uploadListingImage(path: String, bytes: ByteArray, mimeType: String, accessToken: String) {
        require(mimeType in setOf("image/jpeg", "image/png", "image/webp"))
        require(bytes.size <= 10 * 1024 * 1024)
        client.put("${config.baseUrl}/storage/v1/object/listing-images/$path") {
            authHeaders(accessToken)
            contentType(ContentType.parse(mimeType))
            setBody(bytes)
        }
    }

    private fun ListingWire.toModel() = SupabaseListing(id, ownerId, title, description, offerType, priceCents, neighborhood, status)
}
