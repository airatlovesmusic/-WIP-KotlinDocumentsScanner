package com.airatlovesmusic.scanner.ui.scan

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.lifecycle.whenStarted
import com.airatlovesmusic.scanner.R
import kotlinx.android.synthetic.main.fragment_document_scan.*
import kotlinx.coroutines.*
import java.lang.Runnable

class ScanDocumentFragment: Fragment(R.layout.fragment_document_scan) {

    private lateinit var camera: Camera
    private val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initCamera()
        lifecycleScope.launch {
            whenCreated {
                Loader(requireContext()).load {}
            }
            whenStarted {
                while (isActive) {
                    val points = withContext(Dispatchers.IO) { viewFinder.bitmap?.let { GetImageCorners().findDocumentCorners(it) } }
                    if (points != null) { hud.onCornersDetected(points) }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpTapToFocus()
    }

    private fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }
            runCatching {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            }.onFailure { it.printStackTrace() }
        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpTapToFocus() {
        viewFinder.setOnTouchListener { view, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener false
            }

            val factory = DisplayOrientedMeteringPointFactory(
                view.display,
                camera.cameraInfo,
                view.width.toFloat(),
                view.height.toFloat()
            )
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point).build()
            camera.cameraControl.startFocusAndMetering(action)
            return@setOnTouchListener true
        }
    }

}