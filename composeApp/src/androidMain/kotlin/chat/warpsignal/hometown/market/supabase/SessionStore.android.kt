package chat.warpsignal.hometown.market.supabase

import android.content.Context
import android.content.SharedPreferences

private lateinit var preferences: SharedPreferences
private const val PreferencesName = "hometown-market-session"
private const val TokenKey = "access-token"
private const val UserIdKey = "user-id"

fun initializeSessionStore(context: Context) {
    preferences = context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
}

actual fun platformSessionStore(): SessionStore = object : SessionStore {
    override fun restore(): SupabaseSession? {
        if (!::preferences.isInitialized) return null
        val token = preferences.getString(TokenKey, null) ?: return null
        val userId = preferences.getString(UserIdKey, null) ?: return null
        return SupabaseSession(token, userId)
    }
    override fun save(session: SupabaseSession) { preferences.edit().putString(TokenKey, session.accessToken).putString(UserIdKey, session.userId).apply() }
    override fun clear() { preferences.edit().clear().apply() }
}
