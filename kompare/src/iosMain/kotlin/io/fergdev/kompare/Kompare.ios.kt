@file:Suppress("FunctionOnlyReturningConstant")
package io.fergdev.kompare

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi

internal actual suspend fun saveFile(
    imageData: ImageBitmap,
    fileName: String,
    directory: String?
): String? = null
//    TODO("Not yet implemented")

@OptIn(ExperimentalForeignApi::class)
internal actual fun ImageBitmap.readPixelsByteArray(): ByteArray? =
    null
//    TODO("Not yet implemented")
//    val uiImage: UIImage = this.toUIImage() ?: return null // Convert ImageBitmap to UIImage
//    val cgImage: CGImageRef = uiImage.CGImage() ?: return null
//
//    val width = CGImageGetWidth(cgImage).toInt()
//    val height = CGImageGetHeight(cgImage).toInt()
//    val bitsPerComponent = CGImageGetBitsPerComponent(cgImage).toInt()
//    val bitsPerPixel = CGImageGetBitsPerPixel(cgImage).toInt()
//    val bytesPerRow = CGImageGetBytesPerRow(cgImage).toInt()
//    // val colorSpace = CGImageGetColorSpace(cgImage)
//    // val bitmapInfo = CGImageGetBitmapInfo(cgImage) // kCGImageAlphaPremultipliedLast or similar
//
//    // We want a consistent format, ideally ARGB or RGBA.
//    // If the CGImage isn't already in a desired format, we might need to draw it
//    // into a new CGBitmapContext with the desired format. This is complex.
//
//    // Simpler approach: Get the raw data provider.
//    // This might not be in a consistent format across all images.
//    val dataProvider: CGDataProviderRef? = CGImageGetDataProvider(cgImage)
//    if (dataProvider == null) return null
//
//    val pixelDataRef: CFDataRef? = CGDataProviderCopyData(dataProvider)
//    if (pixelDataRef == null) return null
//
//    return try {
//        val length = CFDataGetLength(pixelDataRef).toInt()
//        val bytePtr = CFDataGetBytePtr(pixelDataRef) // Returns an CPointer<UByteVar>
//
//        if (bytePtr == null) return null
//
//        ByteArray(length) { index ->
//            bytePtr[index].toByte()
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//        null
//    } finally {
//        CFRelease(pixelDataRef)
//    }

// For a more robust comparison, you'd ensure both images are converted to a
// canonical pixel format (like 32-bit RGBA) before comparing bytes.
// This might involve creating a new CGBitmapContext, drawing the image into it,
// and then extracting the bytes from that context.
// }
