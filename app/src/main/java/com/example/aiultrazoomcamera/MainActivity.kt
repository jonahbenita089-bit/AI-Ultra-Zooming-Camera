package com.example.aiultrazoomcamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var zoomSeekBar: SeekBar
    private lateinit var zoomValueText: TextView
    private lateinit var qualityText: TextView
    private lateinit var statusText: TextView
    private lateinit var assistantText: TextView
    private lateinit var styleApplyText: TextView
    private lateinit var clearModeButton: MaterialButton
    private lateinit var aiAssistButton: MaterialButton
    private lateinit var captureButton: MaterialButton
    private lateinit var photoModeBtn: MaterialButton
    private lateinit var videoModeBtn: MaterialButton
    private lateinit var modeToggleGroup: MaterialButtonToggleGroup
    private lateinit var styleSpinner: Spinner

    private var camera: Camera? = null
    private var clearModeEnabled = true
    private var currentMode = CaptureMode.PHOTO
    private var selectedStyle = "Natural"

    private enum class CaptureMode {
        PHOTO,
        VIDEO
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            statusText.text = "Camera permission denied. Please enable it."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        zoomSeekBar = findViewById(R.id.zoomSeekBar)
        zoomValueText = findViewById(R.id.zoomValueText)
        qualityText = findViewById(R.id.qualityText)
        statusText = findViewById(R.id.statusText)
        assistantText = findViewById(R.id.assistantText)
        styleApplyText = findViewById(R.id.styleApplyText)
        clearModeButton = findViewById(R.id.clearModeButton)
        aiAssistButton = findViewById(R.id.aiAssistButton)
        captureButton = findViewById(R.id.captureButton)
        photoModeBtn = findViewById(R.id.photoModeBtn)
        videoModeBtn = findViewById(R.id.videoModeBtn)
        modeToggleGroup = findViewById(R.id.modeToggleGroup)
        styleSpinner = findViewById(R.id.styleSpinner)

        setupStyleSpinner()
        setupModeControls()

        clearModeButton.setOnClickListener {
            clearModeEnabled = !clearModeEnabled
            updateDisplay(zoomSeekBar.progress)
        }

        aiAssistButton.setOnClickListener {
            assistantText.text = buildAssistantMessage(selectedStyle)
        }

        captureButton.setOnClickListener {
            val action = if (currentMode == CaptureMode.PHOTO) "Photo captured" else "Video recording started"
            statusText.text = action
            Toast.makeText(this, action, Toast.LENGTH_SHORT).show()
        }

        zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateDisplay(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupModeControls() {
        modeToggleGroup.check(R.id.photoModeBtn)
        currentMode = CaptureMode.PHOTO
        photoModeBtn.setOnClickListener {
            currentMode = CaptureMode.PHOTO
            captureButton.text = "📸 Capture"
            assistantText.text = buildAssistantMessage(selectedStyle)
        }

        videoModeBtn.setOnClickListener {
            currentMode = CaptureMode.VIDEO
            captureButton.text = "🎥 Record"
            assistantText.text = buildAssistantMessage(selectedStyle)
        }
    }

    private fun setupStyleSpinner() {
        val styles = listOf(
            "Natural",
            "Pixar",
            "Cinema 3D",
            "DreamWorks cartoon",
            "Anime",
            "Comic"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            styles
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        styleSpinner.adapter = adapter

        styleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStyle = styles[position]
                styleApplyText.text = "Style: $selectedStyle"
                assistantText.text = buildAssistantMessage(selectedStyle)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun buildAssistantMessage(style: String): String {
        val zoom = zoomSeekBar.progress
        return when (style) {
            "Pixar" -> {
                if (zoom < 50) "Pixar-style soft lighting is warming up the scene." else "Pixar glow enhanced. Character detail and softness are active at $zoom x."
            }
            "Cinema 3D" -> {
                if (zoom < 50) "Cinema 3D depth mapping is calibrating the subject." else "Cinema 3D depth, contrast, and volume are boosting the shot at $zoom x."
            }
            "DreamWorks cartoon" -> {
                if (zoom < 50) "DreamWorks cartoon color pass is enriching the scene." else "DreamWorks pastel detail and cartoon depth are active at $zoom x."
            }
            "Anime" -> {
                if (zoom < 50) "Anime edge polish and color tuning are preparing the frame." else "Anime detail pass is sharpening faces and lines at $zoom x."
            }
            "Comic" -> {
                if (zoom < 50) "Comic pop textures are being applied for bold contrast." else "Comic-style shading and punchy contrast are active at $zoom x."
            }
            else -> {
                if (zoom < 50) "Natural AI enhancement is stabilizing the frame." else "Natural 4K-style detail reconstruction is optimizing the shot at $zoom x."
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            camera?.cameraControl?.setZoomRatio(1f)
            updateDisplay(1)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateDisplay(zoomValue: Int) {
        val safeZoom = zoomValue.coerceIn(1, 1000)
        val zoomRatio = safeZoom.toFloat().coerceIn(1f, 1000f)
        camera?.cameraControl?.setZoomRatio(zoomRatio)

        val qualityPercent = ((safeZoom / 1000f) * 100).toInt().coerceIn(20, 100)
        val statusLabel = if (clearModeEnabled) "Super Clear mode active" else "Classic mode active"

        zoomValueText.text = "Zoom ${safeZoom}x"
        qualityText.text = "AI ${qualityPercent}%"
        statusText.text = statusLabel

        when {
            safeZoom < 10 -> assistantText.text = buildAssistantMessage(selectedStyle)
            safeZoom < 100 -> assistantText.text = buildAssistantMessage(selectedStyle)
            safeZoom < 500 -> assistantText.text = buildAssistantMessage(selectedStyle)
            else -> assistantText.text = buildAssistantMessage(selectedStyle)
        }
    }
}
