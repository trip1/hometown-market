package chat.warpsignal.hometown.market.supabase

import kotlinx.browser.window

private const val TokenKey = "hometown-market.access-token"
private const val UserIdKey = "hometown-market.user-id"

actual fun platformSessionStore(): SessionStore = object : SessionStore {
    override fun restore(): SupabaseSession? {
        val token = window.localStorage.getItem(TokenKey) ?: return null
        val userId = window.localStorage.getItem(UserIdKey) ?: return null
        return SupabaseSession(token, userId)
    }
    override fun save(session: SupabaseSession) {
        window.localStorage.setItem(TokenKey, session.accessToken)
        window.localStorage.setItem(UserIdKey, session.userId)
    }
    override fun clear() {
        window.localStorage.removeItem(TokenKey)
        window.localStorage.removeItem(UserIdKey)
    }
}
