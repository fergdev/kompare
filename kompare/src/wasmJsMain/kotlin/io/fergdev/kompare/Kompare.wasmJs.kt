@file:Suppress("FunctionOnlyReturningConstant")
package io.fergdev.kompare

import androidx.compose.ui.graphics.ImageBitmap

internal actual suspend fun saveFile(
    imageData: ImageBitmap,
    fileName: String,
    directory: String?
): String? = null
//    TODO("Not yet implemented")
// }

internal actual fun ImageBitmap.readPixelsByteArray(): ByteArray? = null
//    TODO("Not yet implemented")
// }
