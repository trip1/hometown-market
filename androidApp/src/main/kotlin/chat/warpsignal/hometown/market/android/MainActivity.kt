package chat.warpsignal.hometown.market.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import chat.warpsignal.hometown.market.HometownMarketApp
import chat.warpsignal.hometown.market.supabase.SupabaseAuthRepository
import chat.warpsignal.hometown.market.supabase.SupabaseConfig
import chat.warpsignal.hometown.market.supabase.initializeSessionStore
import chat.warpsignal.hometown.market.supabase.SupabaseMarketplaceRepository
import chat.warpsignal.hometown.market.supabase.SupabaseServices

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeSessionStore(this)
        val config = SupabaseConfig(publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        val services = SupabaseServices(SupabaseMarketplaceRepository(config), SupabaseAuthRepository(config))
        setContent { HometownMarketApp(services) }
    }
}
