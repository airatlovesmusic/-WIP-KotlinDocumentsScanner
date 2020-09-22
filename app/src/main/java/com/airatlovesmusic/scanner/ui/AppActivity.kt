package com.airatlovesmusic.scanner.ui

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.airatlovesmusic.scanner.R
import com.airatlovesmusic.scanner.entity.Corners
import com.airatlovesmusic.scanner.ui.crop.CropDocumentFragment
import com.airatlovesmusic.scanner.ui.documents.DocumentsFragment
import com.airatlovesmusic.scanner.ui.scan.ScanDocumentFragment

/**
 * Created by Airat Khalilov on 17/09/2020.
 */

class AppActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_container)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, DocumentsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    fun goToScanDocuments() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, ScanDocumentFragment())
            .addToBackStack(null)
            .commit()
    }

    fun goToCrop(uri: Uri, corners: Corners) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, CropDocumentFragment.create(uri, corners))
            .addToBackStack(null)
            .commit()
    }

}