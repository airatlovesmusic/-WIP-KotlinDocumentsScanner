package com.airatlovesmusic.scanner.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.airatlovesmusic.scanner.R

/**
 * Created by Airat Khalilov on 17/09/2020.
 */

class AppActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_container)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment())
                .commit()
        }
    }

}