package chat.warpsignal.hometown.market.supabase

data class SupabaseServices(
    val listings: MarketplaceRepository,
    val auth: SupabaseAuthRepository,
)
