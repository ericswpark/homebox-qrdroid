package com.ericswpark.homebox_qrdroid.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.io.OutputStream
import java.util.EnumMap


const val QR_SIZE = 128

fun getQrCodeMatrix(content: String): BitMatrix {
    val writer = QRCodeWriter()

    val hints: MutableMap<EncodeHintType?, Any?> =
        EnumMap<EncodeHintType?, Any?>(EncodeHintType::class.java)
    hints[EncodeHintType.MARGIN] = 0

    return writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints)
}

fun getTrimmedQrCode(content: String): Bitmap {
    val bitMatrix = getQrCodeMatrix(content)

    var quietZoneWidth = 0
    outer@ for (x in 0 until bitMatrix.width) {
        for (y in 0 until bitMatrix.height) {
            if (bitMatrix[x, y]) {
                // First bit found
                break@outer
            }
        }
        quietZoneWidth += 1
    }

    val width = bitMatrix.width - 2 * quietZoneWidth
    val height = bitMatrix.height - 2 * quietZoneWidth

    val qrBitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in quietZoneWidth until bitMatrix.width - quietZoneWidth) {
        for (y in quietZoneWidth until bitMatrix.height - quietZoneWidth) {
            qrBitmap[x - quietZoneWidth, y - quietZoneWidth] =
                if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }
    }

    return qrBitmap
}

fun getQrCode(content: String): Bitmap {
    val bitMatrix = getQrCodeMatrix(content)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val qrBitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            qrBitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return qrBitmap
}

fun generateQrCode(content: String, label: String, trimQuietZone: Boolean): Bitmap {
    val qrBitmap = if (trimQuietZone) {
        getTrimmedQrCode(content)
    } else {
        getQrCode(content)
    }
    if (label.isBlank()) {
        return qrBitmap
    }

    val width = qrBitmap.width
    val height = qrBitmap.height

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    val textBounds = Rect()
    paint.getTextBounds(label, 0, label.length, textBounds)

    // Have label cut into border (quiet zone) of QR code
    val borderCut = if (trimQuietZone) {0} else {5}

    val labelHeight = textBounds.height() - borderCut

    val finalBitmap = createBitmap(width, height + labelHeight, Bitmap.Config.RGB_565)
    val canvas = Canvas(finalBitmap)

    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(qrBitmap, 0f, 0f, null)

    val textX = canvas.width / 2f
    val textY = (height + textBounds.height() - borderCut).toFloat()
    canvas.drawText(label, textX, textY, paint)

    return finalBitmap
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
