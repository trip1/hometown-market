package chat.warpsignal.hometown.market

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.warpsignal.hometown.market.supabase.CreateListingRequest
import chat.warpsignal.hometown.market.supabase.SupabaseServices
import chat.warpsignal.hometown.market.supabase.SupabaseSession
import kotlinx.coroutines.launch

enum class OfferType(val wire: String, val label: String) { Cash("cash", "Cash"), CashOnly("cash_only", "Cash only"), Trade("trade", "Trade"), TradeOnly("trade_only", "Trade only") }
data class Listing(val title: String, val description: String, val price: String, val offerType: OfferType, val neighborhood: String)

@Composable
fun HometownMarketApp(services: SupabaseServices? = null) {
    val scope = rememberCoroutineScope()
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var session by remember { mutableStateOf<SupabaseSession?>(null) }
    var showingLogin by remember { mutableStateOf(false) }
    var showingPost by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(services) {
        if (services != null) runCatching { services.listings.publicListings() }
            .onSuccess { rows -> listings = rows.map { Listing(it.title, it.description, it.priceCents?.let { cents -> "$${cents / 100}" } ?: "Offer", OfferType.entries.first { type -> type.wire == it.offerType }, it.neighborhood) } }
            .onFailure { error = "Could not load offers" }
    }
    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Hometown Market", color = Color.White, fontWeight = FontWeight.Bold); Text("Buy local. Trade local.", color = Color.White) }
            TextButton(onClick = { showingLogin = session == null }) { Text(if (session == null) "Sign in" else "Signed in", color = Color.White) }
        }
    }, floatingActionButton = { if (session != null) Button(onClick = { showingPost = true }) { Text("Post listing") } }) { padding ->
        when {
            showingLogin && services != null -> LoginScreen(Modifier.padding(padding), services, onSession = { session = it; showingLogin = false }, onCancel = { showingLogin = false })
            showingPost && services != null && session != null -> ListingComposer(Modifier.padding(padding), onCancel = { showingPost = false }) { request ->
                scope.launch { runCatching { services.listings.createListing(request.copy(ownerId = session!!.userId), session!!.accessToken); services.listings.publicListings() }
                    .onSuccess { rows -> listings = rows.map { Listing(it.title, it.description, it.priceCents?.let { cents -> "$${cents / 100}" } ?: "Offer", OfferType.entries.first { type -> type.wire == it.offerType }, it.neighborhood) }; showingPost = false }
                    .onFailure { error = "Could not publish listing" } }
            }
            else -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Browse offers", style = MaterialTheme.typography.headlineSmall)
                Text("No account needed to explore local listings.")
                if (services == null) Text("Supabase client configuration is required to load live listings.")
                listings.forEach { listing -> Text("${listing.title} · ${listing.neighborhood} · ${listing.offerType.label} · ${listing.price}\n${listing.description}") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable private fun LoginScreen(modifier: Modifier, services: SupabaseServices, onSession: (SupabaseSession) -> Unit, onCancel: () -> Unit) {
    val scope = rememberCoroutineScope(); var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Sign in or create account", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Row { Button(onClick = { scope.launch { runCatching { services.auth.signIn(email, password) }.onSuccess(onSession).onFailure { error = "Sign-in failed" } } }) { Text("Sign in") }; Spacer(Modifier.width(8.dp)); Button(onClick = { scope.launch { runCatching { services.auth.signUp(email, password) }.onSuccess(onSession).onFailure { error = "Sign-up failed" } } }) { Text("Create account") } }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable private fun ListingComposer(modifier: Modifier, onCancel: () -> Unit, onPost: (CreateListingRequest) -> Unit) {
    var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var neighborhood by remember { mutableStateOf("Longview") }; var price by remember { mutableStateOf("") }; var offer by remember { mutableStateOf(OfferType.Cash) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Create listing", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(title, { title = it }, label = { Text("What are you offering?") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(neighborhood, { neighborhood = it }, label = { Text("Neighborhood") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(price, { price = it }, label = { Text("Cash price in dollars (optional)") }, modifier = Modifier.fillMaxWidth())
        Row { OfferType.entries.forEach { type -> TextButton(onClick = { offer = type }) { Text(if (offer == type) "• ${type.label}" else type.label) } } }
        Button(enabled = title.length >= 3 && description.length >= 10, onClick = { onPost(CreateListingRequest("", title, description, offer.wire, price.toIntOrNull()?.times(100), neighborhood)) }) { Text("Publish") }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}
