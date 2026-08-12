package chat.warpsignal.hometown.market.supabase

private var cached: SupabaseSession? = null

actual fun platformSessionStore(): SessionStore = object : SessionStore {
    override fun restore(): SupabaseSession? = cached
    override fun save(session: SupabaseSession) { cached = session }
    override fun clear() { cached = null }
}
