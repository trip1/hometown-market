package chat.warpsignal.hometown.market.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import chat.warpsignal.hometown.market.HometownMarketApp
import chat.warpsignal.hometown.market.supabase.SupabaseAuthRepository
import chat.warpsignal.hometown.market.supabase.SupabaseConfig
import chat.warpsignal.hometown.market.supabase.SupabaseMarketplaceRepository
import chat.warpsignal.hometown.market.supabase.SupabaseServices

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val key = configuredPublishableKey()
    val services = key.takeIf { it.isNotBlank() }?.let {
        val config = SupabaseConfig(publishableKey = it)
        SupabaseServices(
            listings = SupabaseMarketplaceRepository(config),
            auth = SupabaseAuthRepository(config),
        )
    }
    ComposeViewport { HometownMarketApp(services) }
}
