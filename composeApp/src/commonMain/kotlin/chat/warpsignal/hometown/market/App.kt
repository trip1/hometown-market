package chat.warpsignal.hometown.market

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.warpsignal.hometown.market.supabase.CreateListingRequest
import chat.warpsignal.hometown.market.supabase.ListingComment
import chat.warpsignal.hometown.market.supabase.ListingImage
import chat.warpsignal.hometown.market.supabase.PickedImage
import chat.warpsignal.hometown.market.supabase.pickListingImage
import chat.warpsignal.hometown.market.supabase.SupabaseServices
import chat.warpsignal.hometown.market.supabase.SupabaseSession
import kotlinx.coroutines.launch

private val Ink = Color(0xFF111113)
private val Canvas = Color(0xFFF7F7F8)
private val Mist = Color(0xFFE9E9ED)
private val Accent = Color(0xFF007AFF)
private val Moss = Color(0xFF1F7A59)

enum class OfferType(val wire: String, val label: String) { Cash("cash", "Cash"), CashOnly("cash_only", "Cash only"), Trade("trade", "Trade"), TradeOnly("trade_only", "Trade only") }
data class Listing(val id: String, val title: String, val description: String, val price: String, val offerType: OfferType, val neighborhood: String)

private fun List<chat.warpsignal.hometown.market.supabase.SupabaseListing>.toUiListings() = map { row ->
    Listing(row.id, row.title, row.description, row.priceCents?.let { "$${it / 100}" } ?: "Make offer", OfferType.entries.firstOrNull { it.wire == row.offerType } ?: OfferType.Trade, row.neighborhood)
}

@Composable
fun HometownMarketApp(services: SupabaseServices? = null) {
    val scope = rememberCoroutineScope()
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var session by remember { mutableStateOf(services?.sessionStore?.restore()) }
    var screen by remember { mutableStateOf("browse") }
    var selectedListing by remember { mutableStateOf<Listing?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(services != null) }
    LaunchedEffect(services) {
        if (services != null) runCatching { services.listings.publicListings() }
            .onSuccess { rows -> listings = rows.toUiListings(); loading = false }
            .onFailure { error = "Couldn’t refresh offers right now."; loading = false }
    }
    Scaffold(containerColor = Canvas, topBar = { AppHeader(session != null, onAccount = { screen = if (session == null) "auth" else "browse" }) }, floatingActionButton = {
        if (session != null && screen == "browse") Button(onClick = { screen = "post" }, colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = CircleShape) { Text("＋ List", modifier = Modifier.padding(horizontal = 4.dp)) }
    }) { inset ->
        when (screen) {
            "auth" -> AuthScreen(Modifier.padding(inset), services, onSession = { authenticated -> services?.sessionStore?.save(authenticated); session = authenticated; screen = "browse" }, onBack = { screen = "browse" })
            "post" -> ListingComposer(Modifier.padding(inset), onCancel = { screen = "browse" }) { request, image ->
                val activeSession = session ?: return@ListingComposer
                val api = services ?: return@ListingComposer
                scope.launch { runCatching {
                    val created = api.listings.createListing(request.copy(ownerId = activeSession.userId), activeSession.accessToken)
                    image?.let { picked ->
                        val path = api.listings.listingImagePath(activeSession.userId, created.id, picked.name)
                        api.listings.uploadListingImage(path, picked.bytes, picked.mimeType, activeSession.accessToken)
                        api.listings.addListingImage(created.id, activeSession.userId, path, activeSession.accessToken)
                    }
                    api.listings.publicListings()
                }.onSuccess { rows -> listings = rows.toUiListings(); screen = "browse" }
                    .onFailure { error = "Couldn’t publish this listing."; screen = "browse" } }
            }
            "detail" -> selectedListing?.let { DetailScreen(Modifier.padding(inset), it, services, session, onBack = { screen = "browse" }) }
            else -> BrowseScreen(Modifier.padding(inset), listings, loading, error, services != null, onSignIn = { screen = "auth" }, onListing = { selectedListing = it; screen = "detail" })
        }
    }
}

@Composable private fun AppHeader(signedIn: Boolean, onAccount: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text("Hometown", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge); Text("local finds, thoughtfully traded", color = Color(0xFF6E6E73), style = MaterialTheme.typography.labelSmall) }
        TextButton(onClick = onAccount, colors = ButtonDefaults.textButtonColors(contentColor = Accent)) { Text(if (signedIn) "Account" else "Sign in", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable private fun BrowseScreen(modifier: Modifier, listings: List<Listing>, loading: Boolean, error: String?, configured: Boolean, onSignIn: () -> Unit, onListing: (Listing) -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(14.dp)); Text("Discover nearby", style = MaterialTheme.typography.headlineLarge, color = Ink, fontWeight = FontWeight.Bold); Text("Beautiful things deserve another story.", color = Color(0xFF6E6E73)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OfferPill("All offers", true); OfferPill("Cash"); OfferPill("Trade"); OfferPill("Trade only") }
        when { loading -> LoadingCard(); !configured -> EmptyCard("Set up Supabase", "This build is missing its public client configuration."); listings.isEmpty() -> EmptyCard("Be the first to list", "Public browsing is open. Sign in to post a local find, a cash offer, or a trade.", onSignIn); else -> listings.take(4).forEach { ListingCard(it, onClick = { onListing(it) }) } }
        error?.let { Text(it, color = Color(0xFFB42318)) }
    }
}

@Composable private fun OfferPill(label: String, selected: Boolean = false) = Text(label, color = if (selected) Color.White else Ink, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clip(CircleShape).background(if (selected) Ink else Mist).padding(horizontal = 14.dp, vertical = 9.dp))

@Composable private fun ListingCard(listing: Listing, onClick: () -> Unit) = Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)).background(if (listing.offerType == OfferType.Trade || listing.offerType == OfferType.TradeOnly) Color(0xFFDDF5E9) else Color(0xFFE4EEFF)), contentAlignment = Alignment.Center) { Text(if (listing.offerType == OfferType.Trade || listing.offerType == OfferType.TradeOnly) "↔" else "$", color = if (listing.offerType == OfferType.Trade || listing.offerType == OfferType.TradeOnly) Moss else Accent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(listing.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${listing.neighborhood} · ${listing.offerType.label}", color = Color(0xFF6E6E73), style = MaterialTheme.typography.labelMedium); Text(listing.description, color = Color(0xFF48484A), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)) }; Spacer(Modifier.width(8.dp)); Text(listing.price, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) } }

@Composable private fun DetailScreen(modifier: Modifier, listing: Listing, services: SupabaseServices?, session: SupabaseSession?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope(); var images by remember { mutableStateOf<List<ListingImage>>(emptyList()) }; var comments by remember { mutableStateOf<List<ListingComment>>(emptyList()) }; var draft by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(listing.id, services) { if (services != null) runCatching { Pair(services.listings.listingImages(listing.id), services.listings.listingComments(listing.id)) }.onSuccess { (loadedImages, loadedComments) -> images = loadedImages; comments = loadedComments }.onFailure { error = "Couldn’t load listing details." } }
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("‹ Back to offers", color = Accent) }; Text(listing.title, style = MaterialTheme.typography.headlineLarge, color = Ink, fontWeight = FontWeight.Bold); Text("${listing.neighborhood} · ${listing.offerType.label} · ${listing.price}", color = Color(0xFF6E6E73)); Text(listing.description, color = Ink)
        if (images.isEmpty()) Text("No photos yet", color = Color(0xFF6E6E73)) else Text("${images.size} photo${if (images.size == 1) "" else "s"} attached", color = Moss)
        Text("Comments", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold); if (comments.isEmpty()) Text("No comments yet.", color = Color(0xFF6E6E73)) else comments.forEach { Text(it.body, color = Ink, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(12.dp)) }
        if (session != null && services != null) { OutlinedTextField(draft, { draft = it }, label = { Text("Add a comment") }, modifier = Modifier.fillMaxWidth()); Button(enabled = draft.isNotBlank(), onClick = { scope.launch { runCatching { services.listings.addComment(listing.id, draft.trim(), session.accessToken); services.listings.listingComments(listing.id) }.onSuccess { comments = it; draft = "" }.onFailure { error = "Couldn’t post comment." } } }, colors = ButtonDefaults.buttonColors(containerColor = Ink)) { Text("Post comment") } } else Text("Sign in to join the conversation.", color = Color(0xFF6E6E73)); error?.let { Text(it, color = Color(0xFFB42318)) }
    }
}

@Composable private fun EmptyCard(title: String, body: String, action: (() -> Unit)? = null) = Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp)) { Text("✦", color = Accent, style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(10.dp)); Text(title, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(body, color = Color(0xFF6E6E73), modifier = Modifier.padding(top = 6.dp)); action?.let { Button(onClick = it, colors = ButtonDefaults.buttonColors(containerColor = Ink), modifier = Modifier.padding(top = 16.dp)) { Text("Sign in to list") } } } }
@Composable private fun LoadingCard() = EmptyCard("Finding local offers", "Refreshing the latest neighborhood listings…")

@Composable private fun AuthScreen(modifier: Modifier, services: SupabaseServices?, onSession: (SupabaseSession) -> Unit, onBack: () -> Unit) { val scope = rememberCoroutineScope(); var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }; Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { TextButton(onClick = onBack) { Text("‹ Back", color = Accent) }; Text("Welcome back", style = MaterialTheme.typography.headlineLarge, color = Ink, fontWeight = FontWeight.Bold); Text("Sign in to list, trade, and comment.", color = Color(0xFF6E6E73)); OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true); OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Button(onClick = { if (services != null) scope.launch { runCatching { services.auth.signIn(email, password) }.onSuccess(onSession).onFailure { error = "Sign-in failed. Check your email and password." } } }, colors = ButtonDefaults.buttonColors(containerColor = Ink), modifier = Modifier.fillMaxWidth()) { Text("Sign in") }; TextButton(onClick = { if (services != null) scope.launch { runCatching { services.auth.signUp(email, password) }.onSuccess(onSession).onFailure { error = "Couldn’t create the account." } } }) { Text("New here? Create an account", color = Accent) }; error?.let { Text(it, color = Color(0xFFB42318)) } } }

@Composable private fun ListingComposer(modifier: Modifier, onCancel: () -> Unit, onPost: (CreateListingRequest, PickedImage?) -> Unit) { val scope = rememberCoroutineScope(); var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var neighborhood by remember { mutableStateOf("Longview") }; var price by remember { mutableStateOf("") }; var offer by remember { mutableStateOf(OfferType.Cash) }; var image by remember { mutableStateOf<PickedImage?>(null) }; Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { TextButton(onClick = onCancel) { Text("‹ Cancel", color = Accent) }; Text("List something good", style = MaterialTheme.typography.headlineLarge, color = Ink, fontWeight = FontWeight.Bold); Text("Tell neighbors what you have and how you’d like to trade.", color = Color(0xFF6E6E73)); OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3); OutlinedTextField(neighborhood, { neighborhood = it }, label = { Text("Neighborhood") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(price, { price = it }, label = { Text("Cash price (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { OfferType.entries.forEach { type -> Text(type.label, modifier = Modifier.clickable { offer = type }.clip(CircleShape).background(if (offer == type) Ink else Mist).padding(horizontal = 10.dp, vertical = 8.dp), color = if (offer == type) Color.White else Ink, style = MaterialTheme.typography.labelSmall) } }; TextButton(onClick = { scope.launch { image = pickListingImage() } }) { Text(image?.let { "Photo: ${it.name}" } ?: "Add photo", color = Accent) }; Button(enabled = title.length >= 3 && description.length >= 10, onClick = { onPost(CreateListingRequest("", title, description, offer.wire, price.toIntOrNull()?.times(100), neighborhood), image) }, colors = ButtonDefaults.buttonColors(containerColor = Ink), modifier = Modifier.fillMaxWidth()) { Text("Publish listing") } } }
