package com.ericswpark.homebox_qrdroid.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.OutputStream

fun generateQrCode(content: String): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return bitmap
}

fun saveQrCodeToStorage(context: Context, bitmap: Bitmap, displayName: String) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/homebox-qrdroid")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

    uri?.let {
        val out: OutputStream? = resolver.openOutputStream(it)
        out?.let {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.close()
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(it, values, null, null)
    }

    Toast.makeText(context, "Saved $displayName", Toast.LENGTH_SHORT).show()
}
