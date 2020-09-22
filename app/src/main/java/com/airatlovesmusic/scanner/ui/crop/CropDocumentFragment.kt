package com.airatlovesmusic.scanner.ui.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.net.toFile
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.airatlovesmusic.scanner.R
import com.airatlovesmusic.scanner.entity.Corners
import com.airatlovesmusic.scanner.model.opencv.TransformDocumentImage
import kotlinx.android.synthetic.main.fragment_crop.*

class CropDocumentFragment: Fragment(R.layout.fragment_crop) {

    private val uri by lazy { requireArguments().getParcelable<Uri>(ARG_URI) as Uri }
    private val corners by lazy { requireArguments().getParcelable<Corners>(ARG_CORNERS) as Corners }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hud.cropMode = true
        iv_preview.setImageURI(uri)
        hud.post { hud.onCornersDetected(corners) }
        btn_crop.setOnClickListener {
            val preview = BitmapFactory.decodeFile(uri.toFile().name)
            val bitmap = TransformDocumentImage().getTranformedDocumentImage(preview, hud.getPoints())
            hud.onCornersNotDetected()
            iv_preview.setImageBitmap(bitmap)
        }
        btn_rescan.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    companion object {
        fun create(uri: Uri, corners: Corners) = CropDocumentFragment().apply {
            arguments = bundleOf(
                ARG_URI to uri,
                ARG_CORNERS to corners
            )
        }
        const val ARG_URI = "bitmap"
        const val ARG_CORNERS = "corners"
    }

}