package chat.warpsignal.hometown.market.supabase

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun pickListingImage(): PickedImage? = suspendCancellableCoroutine { continuation ->
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = "image/jpeg,image/png,image/webp"
    input.onchange = {
        val file = input.files?.item(0)
        if (file == null) {
            continuation.resume(null)
        } else {
            val reader = FileReader()
            reader.onload = {
                val dataUrl = reader.result?.toString()
                val encoded = dataUrl?.substringAfter(',', missingDelimiterValue = "")
                continuation.resume(encoded?.takeIf { it.isNotBlank() }?.let { bytes ->
                    runCatching { PickedImage(file.name, file.type, Base64.decode(bytes)) }.getOrNull()
                })
            }
            reader.onerror = { continuation.resume(null) }
            reader.readAsDataURL(file)
        }
    }
    input.click()
}
