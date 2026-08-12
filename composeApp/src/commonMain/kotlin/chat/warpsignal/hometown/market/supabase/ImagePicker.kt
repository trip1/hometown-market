package chat.warpsignal.hometown.market.supabase

data class PickedImage(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
)

expect suspend fun pickListingImage(): PickedImage?
