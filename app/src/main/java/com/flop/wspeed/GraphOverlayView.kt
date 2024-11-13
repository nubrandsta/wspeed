package com.flop.wspeed

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GraphOverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var pixelIntensities = mutableListOf<Double>()
    private val graphPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 4F
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGraph(canvas)
    }

    private fun drawGraph(canvas: Canvas) {
        if (pixelIntensities.isEmpty()) return

        val graphHeight = height.toFloat()
        val maxIntensity = pixelIntensities.maxOrNull() ?: 1.0
        val scaleX = width.toFloat() / pixelIntensities.size
        val scaleY = graphHeight / maxIntensity.toFloat()

        for (i in 1 until pixelIntensities.size) {
            val startX = (i - 1) * scaleX
            val startY = graphHeight - (pixelIntensities[i - 1] * scaleY).toFloat()
            val stopX = i * scaleX
            val stopY = graphHeight - (pixelIntensities[i] * scaleY).toFloat()
            canvas.drawLine(startX, startY, stopX, stopY, graphPaint)
        }
    }

    fun updatePixelIntensities(newIntensities: List<Double>) {
        pixelIntensities = newIntensities.toMutableList()
        invalidate()
    }
}