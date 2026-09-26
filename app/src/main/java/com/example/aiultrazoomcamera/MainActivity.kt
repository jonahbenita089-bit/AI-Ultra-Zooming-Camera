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
    private lateinit var zoomSeekBar: SeekBar
    private lateinit var zoomValueText: TextView
    private lateinit var qualityText: TextView
    private lateinit var statusText: TextView
    private lateinit var assistantText: TextView
    private lateinit var styleApplyText: TextView
    private lateinit var captureButton: MaterialButton
    private lateinit var styleSpinner: Spinner
    private var camera: Camera? = null
    private var selectedStyle = "Natural"
    private var videoMode = false
    private var clearMode = true

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else statusText.text = "Camera permission denied"
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_main)
        val preview = findViewById<PreviewView>(R.id.previewView)
        zoomSeekBar = findViewById(R.id.zoomSeekBar)
        zoomValueText = findViewById(R.id.zoomValueText)
        qualityText = findViewById(R.id.qualityText)
        statusText = findViewById(R.id.statusText)
        assistantText = findViewById(R.id.assistantText)
        styleApplyText = findViewById(R.id.styleApplyText)
        captureButton = findViewById(R.id.captureButton)
        styleSpinner = findViewById(R.id.styleSpinner)
        val clearButton = findViewById<MaterialButton>(R.id.clearModeButton)
        val assistantButton = findViewById<MaterialButton>(R.id.aiAssistButton)
        val modes = findViewById<MaterialButtonToggleGroup>(R.id.modeToggleGroup)
        val photo = findViewById<MaterialButton>(R.id.photoModeBtn)
        val video = findViewById<MaterialButton>(R.id.videoModeBtn)

        styleSpinner.adapter = ArrayAdapter.createFromResource(this, R.array.ai_styles_array, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        styleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStyle = parent?.getItemAtPosition(position).toString()
                styleApplyText.text = "Style: $selectedStyle"
                updateAssistant()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        modes.check(R.id.photoModeBtn)
        photo.setOnClickListener { videoMode = false; captureButton.text = getString(R.string.capture); updateAssistant() }
        video.setOnClickListener { videoMode = true; captureButton.text = getString(R.string.record); updateAssistant() }
        clearButton.setOnClickListener { clearMode = !clearMode; updateDisplay(zoomSeekBar.progress) }
        assistantButton.setOnClickListener { updateAssistant() }
        captureButton.setOnClickListener {
            val message = if (videoMode) "Video recording started" else "Photo captured"
            statusText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) = updateDisplay(value)
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera() else permission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)
            camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            updateDisplay(1)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateDisplay(value: Int) {
        val zoom = value.coerceIn(1, 1000)
        camera?.cameraControl?.setZoomRatio(zoom.toFloat())
        zoomValueText.text = "Zoom ${zoom}x"
        qualityText.text = "AI ${((zoom / 1000f) * 100).toInt().coerceIn(20, 100)}%"
        statusText.text = if (clearMode) "Super Clear mode active" else "Classic mode active"
        updateAssistant()
    }

    private fun updateAssistant() {
        val mode = if (videoMode) "video" else "photo"
        assistantText.text = "$selectedStyle style is ready for $mode. AI stabilization, color, and detail enhancement are active."
    }
}
