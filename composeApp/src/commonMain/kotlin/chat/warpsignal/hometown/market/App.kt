package chat.warpsignal.hometown.market

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class OfferType(val label: String) { Cash("Cash"), CashOnly("Cash only"), Trade("Trade"), TradeOnly("Trade only") }
data class Listing(val id: String, val title: String, val description: String, val price: String, val offerType: OfferType, val imageUrl: String, val neighborhood: String)

private val sampleListings = listOf(
    Listing("1", "Vintage Raleigh road bike", "Fresh tune-up. Would trade for a cargo bike accessory.", "$180", OfferType.Trade, "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800", "Longview"),
    Listing("2", "Solid oak bookshelf", "Six shelves, good condition.", "$65", OfferType.CashOnly, "https://images.unsplash.com/photo-1594620302200-9a762244a156?w=800", "East Texas"),
    Listing("3", "Cast iron skillet set", "Two skillets, seasoned and ready.", "Make an offer", OfferType.TradeOnly, "https://images.unsplash.com/photo-1584990347449-a0c089b19c08?w=800", "Longview"),
)

@Composable
fun HometownMarketApp() {
    var listings by remember { mutableStateOf(sampleListings) }
    var signedIn by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<OfferType?>(null) }
    var showingPost by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Hometown Market", color = Color.White, fontWeight = FontWeight.Bold); Text("Buy local. Trade local.", color = Color.White) }
            TextButton(onClick = { signedIn = !signedIn }) { Text(if (signedIn) "Demo user" else "Sign in", color = Color.White) }
        }
    }, floatingActionButton = {
        if (signedIn) Button(onClick = { showingPost = true }) { Text("Post listing") }
    }) { padding ->
        if (showingPost) {
            ListingComposer(Modifier.padding(padding), onCancel = { showingPost = false }) { listing ->
                listings = listOf(listing) + listings
                showingPost = false
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Browse offers", style = MaterialTheme.typography.headlineSmall)
                Text("No account needed to explore local listings.")
                Text("Offer types: cash · cash only · trade · trade only", modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listings.forEach { ListingCard(it) }
                }
            }
        }
    }
}

@Composable private fun ListingCard(listing: Listing) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(listing.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${listing.neighborhood} · ${listing.offerType.label}", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp)); Text(listing.description)
            Spacer(Modifier.height(8.dp)); Text(listing.price, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun ListingComposer(modifier: Modifier, onCancel: () -> Unit, onPost: (Listing) -> Unit) {
    var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }; var imageUrl by remember { mutableStateOf("") }
    var offer by remember { mutableStateOf(OfferType.Cash) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Create listing", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(title, { title = it }, label = { Text("What are you offering?") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(price, { price = it }, label = { Text("Cash price or offer") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(imageUrl, { imageUrl = it }, label = { Text("Photo URL (native upload next)") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { OfferType.entries.forEach { type -> FilterChip(offer == type, { offer = type }, label = { Text(type.label) }) } }
        Row { Button(enabled = title.isNotBlank() && description.isNotBlank(), onClick = { onPost(Listing("local-${title.hashCode()}", title, description, price.ifBlank { "Offer" }, offer, imageUrl, "Your neighborhood")) }) { Text("Publish locally") }; Spacer(Modifier.width(8.dp)); TextButton(onClick = onCancel) { Text("Cancel") } }
    }
}
