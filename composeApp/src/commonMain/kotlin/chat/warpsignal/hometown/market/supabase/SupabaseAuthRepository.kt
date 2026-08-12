package chat.warpsignal.hometown.market.supabase

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.contentType
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SupabaseSession(val accessToken: String, val userId: String)

@Serializable
private data class AuthWire(
    @SerialName("access_token") val accessToken: String,
    val user: UserWire,
)
@Serializable private data class UserWire(val id: String)

class SupabaseAuthRepository(
    private val config: SupabaseConfig,
    private val client: HttpClient = platformHttpClient().config { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
) {
    suspend fun signIn(email: String, password: String): SupabaseSession = authenticate("token?grant_type=password", mapOf("email" to email, "password" to password))
    suspend fun signUp(email: String, password: String): SupabaseSession = authenticate("signup", mapOf("email" to email, "password" to password))

    private suspend fun authenticate(path: String, payload: Map<String, String>): SupabaseSession {
        val response = client.post("${config.baseUrl}/auth/v1/$path") {
            header("apikey", config.publishableKey)
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body<AuthWire>()
        return SupabaseSession(response.accessToken, response.user.id)
    }
}
