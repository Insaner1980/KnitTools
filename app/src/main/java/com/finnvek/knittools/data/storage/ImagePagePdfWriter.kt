package com.finnvek.knittools.data.storage

import android.graphics.Bitmap
import java.io.DataOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/** Image-only PDF. The output retains object offsets, never earlier pages' pixel data. */
internal class ImagePagePdfWriter(
    private val output: DataOutputStream,
) {
    private val offsets = mutableListOf<Int>()
    private val pageIds = mutableListOf<Int>()

    init {
        output.writeBytes("%PDF-1.4\n")
        objectHeader()
        output.writeBytes("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        offsets.add(0) // The page tree is object 2 and is written after its children.
    }

    fun writePage(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val page = objectHeader()
        pageIds.add(page)
        output.writeBytes(
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $width $height] " +
                "/Contents ${page + 1} 0 R /Resources << /XObject << /Image ${page + 2} 0 R >> >> >>\nendobj\n",
        )
        val paint = "q $width 0 0 $height 0 0 cm /Image Do Q\n"
        objectHeader()
        output.writeBytes("<< /Length ${paint.length} >>\nstream\n${paint}endstream\nendobj\n")
        objectHeader()
        output.writeBytes(
            "<< /Type /XObject /Subtype /Image /Width $width /Height $height " +
                "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length ${page + 3} 0 R >>\nstream\n",
        )
        val start = output.size()
        writePixels(bitmap)
        val length = output.size() - start
        output.writeBytes("\nendstream\nendobj\n")
        objectHeader()
        output.writeBytes("$length\nendobj\n")
    }

    fun finish() {
        offsets[1] = output.size()
        val children = pageIds.joinToString(" ") { "$it 0 R" }
        output.writeBytes("2 0 obj\n<< /Type /Pages /Count ${pageIds.size} /Kids [$children] >>\nendobj\n")
        val xref = output.size()
        val count = offsets.size + 1
        output.writeBytes("xref\n0 $count\n0000000000 65535 f \n")
        for (offset in offsets) output.writeBytes("${offset.toString().padStart(10, '0')} 00000 n \n")
        output.writeBytes("trailer\n<< /Size $count /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
    }

    private fun objectHeader(): Int {
        offsets.add(output.size())
        output.writeBytes("${offsets.size} 0 obj\n")
        return offsets.size
    }

    private fun writePixels(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width)
        val rgb = ByteArray(bitmap.width * 3)
        val deflater = Deflater()
        try {
            val stream = DeflaterOutputStream(output, deflater, 32 * 1024)
            for (y in 0 until bitmap.height) {
                bitmap.getPixels(pixels, 0, pixels.size, 0, y, pixels.size, 1)
                var index = 0
                for (pixel in pixels) {
                    val alpha = pixel ushr 24
                    rgb[index++] = overWhite(pixel ushr 16, alpha)
                    rgb[index++] = overWhite(pixel ushr 8, alpha)
                    rgb[index++] = overWhite(pixel, alpha)
                }
                stream.write(rgb)
            }
            stream.finish()
        } finally {
            deflater.end()
        }
    }

    private fun overWhite(
        channel: Int,
        alpha: Int,
    ): Byte {
        if (alpha == 255) return channel.toByte()
        return (255 - alpha + ((channel and 255) * alpha + 127) / 255).toByte()
    }
}
