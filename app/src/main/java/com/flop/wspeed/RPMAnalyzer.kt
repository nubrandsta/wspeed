package com.flop.wspeed
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class RPMAnalyzer(private val numBlades: Int) : ImageAnalysis.Analyzer {

    private var previousFrame: Bitmap? = null
    private val roi = Rect(100, 100, 200, 200) // Example ROI

    override fun analyze(image: ImageProxy) {
        val bitmap = image.toBitmap()
        val currentFrame = Bitmap.createBitmap(bitmap, roi.left, roi.top, roi.width(), roi.height())

        if (previousFrame != null) {
            val diff = calculateFrameDifference(previousFrame!!, currentFrame)
            if (diff > THRESHOLD) {
                // Detected a peak
                // Store the frame number or timestamp
            }
        }

        previousFrame = currentFrame
        image.close()
    }

    private fun calculateFrameDifference(frame1: Bitmap, frame2: Bitmap): Int {
        var diff = 0
        for (x in 0 until frame1.width) {
            for (y in 0 until frame1.height) {
                val pixel1 = frame1.getPixel(x, y)
                val pixel2 = frame2.getPixel(x, y)
                diff += Color.red(pixel1) - Color.red(pixel2)
            }
        }
        return diff
    }

    companion object {
        private const val THRESHOLD = 50 // Example threshold
    }
}