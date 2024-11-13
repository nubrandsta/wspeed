package com.flop.wspeed

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var previousFrame: Bitmap? = null
    private val pixelDiffs = mutableListOf<Double>()
    private val peakFrames = mutableListOf<Int>()
    private var frameNumber = 0
    private val fps = 30  // Set your actual video FPS
    private val bladeCount = 3  // Adjust based on the number of fan blades

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.view_finder).surfaceProvider)
            }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, FrameAnalyzer())
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageAnalyzer)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    inner class FrameAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            val bitmap = image.toBitmap()
            val currentFrame = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height)

            // Convert frame to grayscale
            val grayscaleFrame = Bitmap.createBitmap(currentFrame.width, currentFrame.height, Bitmap.Config.ARGB_8888)
            for (x in 0 until currentFrame.width) {
                for (y in 0 until currentFrame.height) {
                    val pixel = currentFrame.getPixel(x, y)
                    val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                    grayscaleFrame.setPixel(x, y, Color.rgb(gray, gray, gray))
                }
            }

            // Set up the ROI
            val xFraction = 0.7
            val yFraction = 0.7
            val widthFraction = 0.01
            val heightFraction = 0.01

            val roiX = (xFraction * grayscaleFrame.width).toInt()
            val roiY = (yFraction * grayscaleFrame.height).toInt()
            val roiWidth = (widthFraction * grayscaleFrame.width).toInt()
            val roiHeight = (heightFraction * grayscaleFrame.height).toInt()

            // Calculate pixel intensity differences
            val pixelIntensities = mutableListOf<Double>()
            for (x in roiX until roiX + roiWidth) {
                for (y in roiY until roiY + roiHeight) {
                    val currentPixel = grayscaleFrame.getPixel(x, y)
                    pixelIntensities.add(Color.red(currentPixel).toDouble())
                }
            }

            // Update the overlay with the new pixel intensities
            findViewById<GraphOverlayView>(R.id.graph_overlay).updatePixelIntensities(pixelIntensities)

            // Update previous frame and increment frame number
            previousFrame = grayscaleFrame
            frameNumber++

            image.close()
        }
    }
}