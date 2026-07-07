package com.kelompok1.simo.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kelompok1.simo.R
import com.kelompok1.simo.databinding.ActivityMainBinding
import com.kelompok1.simo.ui.common.PlaceholderFragment
import com.kelompok1.simo.ui.dashboard.DashboardFragment
import com.kelompok1.simo.ui.lainnya.LainnyaFragment
import com.kelompok1.simo.util.SessionCache
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var session: SessionCache
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val role = session.roleName ?: "cost_accounting"
        val menuRes = when (role) {
            "it_support" -> R.menu.menu_it
            "djka" -> R.menu.menu_djka
            else -> R.menu.menu_cost
        }
        binding.bottomNav.menu.clear()
        binding.bottomNav.inflateMenu(menuRes)

        binding.bottomNav.setOnItemSelectedListener { item ->
            val (fragment, title) = fragmentFor(item.itemId)
            supportActionBar?.title = title
            showFragment(fragment)
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_dashboard
        }
    }

    private fun fragmentFor(itemId: Int): Pair<Fragment, String> = when (itemId) {
        R.id.nav_dashboard -> DashboardFragment() to "Dashboard"
        R.id.nav_kontrak -> PlaceholderFragment.new("Kontrak IMO") to "Kontrak"
        R.id.nav_biaya -> PlaceholderFragment.new("Kalkulasi Biaya") to "Biaya"
        R.id.nav_billing -> PlaceholderFragment.new("Billing & Tagihan") to "Billing"
        R.id.nav_user -> PlaceholderFragment.new("Kelola User") to "Kelola User"
        R.id.nav_role -> PlaceholderFragment.new("Kelola Role") to "Kelola Role"
        R.id.nav_verifikasi -> PlaceholderFragment.new("Verifikasi Tagihan") to "Verifikasi"
        R.id.nav_lainnya -> LainnyaFragment() to "Lainnya"
        else -> DashboardFragment() to "Dashboard"
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
