package com.airatlovesmusic.scanner.ui.crop

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.airatlovesmusic.scanner.R
import com.airatlovesmusic.scanner.entity.Corners
import com.airatlovesmusic.scanner.model.TransformImage
import kotlinx.android.synthetic.main.fragment_crop.*

class CropDocumentFragment: Fragment(R.layout.fragment_crop) {

    private val preview by lazy { requireArguments().getParcelable<Bitmap>(ARG_BITMAP) as Bitmap }
    private val corners by lazy { requireArguments().getParcelable<Corners>(ARG_CORNERS) as Corners }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hud.cropMode = true
        iv_preview.setImageBitmap(preview)
        hud.post { hud.onCornersDetected(corners) }
        btn_crop.setOnClickListener {
            val bitmap = TransformImage().getTranformedDocument(preview, hud.getPoints())
            hud.onCornersNotDetected()
            iv_preview.setImageBitmap(bitmap)
        }
        btn_rescan.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    companion object {
        fun create(bitmap: Bitmap, corners: Corners) = CropDocumentFragment().apply {
            arguments = bundleOf(
                ARG_BITMAP to bitmap,
                ARG_CORNERS to corners
            )
        }
        const val ARG_BITMAP = "bitmap"
        const val ARG_CORNERS = "corners"
    }

}