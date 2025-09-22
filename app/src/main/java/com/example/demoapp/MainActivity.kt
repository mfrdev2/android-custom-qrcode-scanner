package com.example.demoapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        previewView = findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrCode ->
                        runOnUiThread {
                            processQRCode(qrCode)
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private var isScanning = true
    private var lastScannedCode: String? = null

    private fun processQRCode(qrCode: String) {
        // Prevent multiple scans of the same code
        if (!isScanning || qrCode == lastScannedCode) return

        // Pause scanning
        pauseScanning()
        lastScannedCode = qrCode
        Toast.makeText(this, "QR Code: $qrCode", Toast.LENGTH_LONG).show()
        // Show confirmation dialog with retry option
        showScanResultDialog(qrCode)
    }
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED


    private fun pauseScanning() {
        isScanning = false
        imageAnalyzer?.clearAnalyzer()
    }

    private fun resumeScanning() {
        isScanning = true
        lastScannedCode = null
        imageAnalyzer?.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrCode ->
            runOnUiThread {
                processQRCode(qrCode)
            }
        })
        runOnUiThread {

        }
    }

    private fun showScanResultDialog(qrCode: String) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("QR Code Scanned")
            .setMessage("Scanned Code: $qrCode\n\nWhat would you like to do?")
            .setCancelable(false)
            .setPositiveButton("Process") { _, _ ->
                // Add your custom processing logic here
                processMemberIdentity(qrCode)
            }
            .setNegativeButton("Retry") { _, _ ->
                // Resume scanning for retry
                resumeScanning()
            }
            .setNeutralButton("Cancel") { _, _ ->
                // Just resume scanning without processing
                resumeScanning()
            }
            .create()

        dialog.show()

        // Auto-dismiss after 15 seconds and resume scanning
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
                resumeScanning()
            }
        }, 15000)
    }


    private fun processMemberIdentity(qrCode: String) {
        // Show processing indicator
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Processing...")
            .setMessage("Validating member identity...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        // Simulate processing (replace with your actual logic)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            progressDialog.dismiss()

            // Simulate processing result
            val isValid = validateQRCode(qrCode)

            if (isValid) {
                showSuccessDialog(qrCode)
            } else {
                showErrorDialog(qrCode)
            }
        }, 2000)
    }

    private fun validateQRCode(qrCode: String): Boolean {
        // Add your validation logic here
        // For example: check format, validate with server, etc.

        // Simulate validation (replace with actual logic)
        return qrCode.isNotEmpty() && qrCode.length >= 10
    }

    private fun showSuccessDialog(qrCode: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("Member identity validated successfully!\n\nCode: $qrCode")
            .setPositiveButton("Scan Another") { _, _ ->
                resumeScanning()
            }
            .setNegativeButton("Done") { _, _ ->
                // Optionally finish activity or stay paused
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(qrCode: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Validation Failed")
            .setMessage("Invalid member identity code.\n\nCode: $qrCode")
            .setPositiveButton("Retry Scan") { _, _ ->
                resumeScanning()
            }
            .setNegativeButton("Try Again") { _, _ ->
                // Reprocess the same code
                processMemberIdentity(qrCode)
            }
            .setNeutralButton("Cancel") { _, _ ->
                resumeScanning()
            }
            .setCancelable(false)
            .show()
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}



// QRCodeAnalyzer.kt
class QRCodeAnalyzer(private val onQRCodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        when (barcode.valueType) {
                            Barcode.TYPE_TEXT -> {
                                barcode.displayValue?.let { onQRCodeDetected(it) }
                            }
                            Barcode.TYPE_URL -> {
                                barcode.url?.url?.let { onQRCodeDetected(it) }
                            }
                            else -> {
                                barcode.rawValue?.let { onQRCodeDetected(it) }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // Handle error
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}