package chat.warpsignal.hometown.market.supabase

import io.ktor.client.HttpClient

/** Platform-specific HTTP engine selection. Shared repositories must not rely on runtime engine discovery. */
expect fun platformHttpClient(): HttpClient
