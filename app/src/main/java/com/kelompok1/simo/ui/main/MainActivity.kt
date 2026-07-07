package com.kelompok1.simo.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kelompok1.simo.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Placeholder MainActivity untuk Fase 0 (memastikan proyek bisa di-build & dijalankan).
 * Akan digantikan alur Splash -> Login -> Main pada Fase 2.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
