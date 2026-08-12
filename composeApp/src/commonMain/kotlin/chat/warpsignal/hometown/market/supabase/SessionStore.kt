package chat.warpsignal.hometown.market.supabase

interface SessionStore {
    fun restore(): SupabaseSession?
    fun save(session: SupabaseSession)
    fun clear()
}

expect fun platformSessionStore(): SessionStore
