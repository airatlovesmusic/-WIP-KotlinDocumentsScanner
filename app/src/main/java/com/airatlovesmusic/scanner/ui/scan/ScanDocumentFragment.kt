package com.airatlovesmusic.scanner.ui.scan

import android.os.Bundle
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airatlovesmusic.scanner.R
import kotlinx.android.synthetic.main.fragment_document_scan.*

class ScanDocumentFragment: Fragment(R.layout.fragment_document_scan) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initCamera()
    }

    private fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            runCatching {
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            }.onFailure { it.printStackTrace() }
        }, ContextCompat.getMainExecutor(requireActivity()))
    }

}