package chat.warpsignal.hometown.market.supabase

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun platformHttpClient(): HttpClient = HttpClient(Js)
