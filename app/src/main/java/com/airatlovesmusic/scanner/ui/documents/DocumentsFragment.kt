package com.airatlovesmusic.scanner.ui.documents

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.airatlovesmusic.scanner.R
import com.airatlovesmusic.scanner.entity.Document
import com.airatlovesmusic.scanner.ui.AppActivity
import com.airatlovesmusic.scanner.ui.documents.adapter.DocumentsAdapter
import com.airatlovesmusic.scanner.ui.scan.ScanDocumentFragment
import kotlinx.android.synthetic.main.fragment_documents.*

/**
 * Created by Airat Khalilov on 17/09/2020.
 */

class DocumentsFragment: Fragment(R.layout.fragment_documents) {

    private val adapter by lazy {
        DocumentsAdapter().apply { updateList((0..10).map { Document() }) }
    }

    private val askPermissionForScanner: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.all { it.value }) (activity as? AppActivity)?.goToScanDocuments()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecycler()
        fab_new.setOnClickListener {
            askPermissionForScanner.launch(
                arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }
    }

    private fun initRecycler() {
        rv_documents.layoutManager = LinearLayoutManager(context)
        rv_documents.adapter = adapter
        rv_documents.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

}