package com.kelompok1.simo.ui.main

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.kelompok1.simo.R
import com.kelompok1.simo.databinding.ActivityMainBinding
import com.kelompok1.simo.databinding.NavHeaderBinding
import com.kelompok1.simo.ui.aset.AsetFragment
import com.kelompok1.simo.ui.biaya.BiayaFragment
import com.kelompok1.simo.ui.billing.BillingFragment
import com.kelompok1.simo.ui.dashboard.AktivitasFragment
import com.kelompok1.simo.ui.dashboard.DashboardFragment
import com.kelompok1.simo.ui.hakakses.HakAksesFragment
import com.kelompok1.simo.ui.kontrak.KontrakFragment
import com.kelompok1.simo.ui.lainnya.LainnyaFragment
import com.kelompok1.simo.ui.sinkronisasi.SinkronisasiFragment
import com.kelompok1.simo.util.SessionCache
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    @Inject lateinit var session: SessionCache
    private lateinit var binding: ActivityMainBinding

    /** Fragment back-stack manual (untuk back ≠ keluar) */
    private val fragmentStack = ArrayDeque<Pair<Fragment, String>>()

    /** Timestamp back-press pertama untuk double-back-exit */
    private var backPressedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.nav_open, R.string.nav_close
        )
        toggle.drawerArrowDrawable.color = getColor(R.color.white)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.toolbar.setNavigationIconTint(getColor(R.color.white))

        // Nav menu sesuai role
        val role = session.roleName ?: "cost_accounting"
        val menuRes = when (role) {
            "djka"    -> R.menu.menu_drawer_djka
            "sap_erp" -> R.menu.menu_drawer_sap
            else      -> R.menu.menu_drawer_cost
        }
        binding.navView.menu.clear()
        binding.navView.inflateMenu(menuRes)
        binding.navView.setNavigationItemSelectedListener(this)

        // Header drawer
        val headerView = binding.navView.getHeaderView(0)
        val header = NavHeaderBinding.bind(headerView)
        header.tvNavNama.text = session.namaLengkap ?: "Pengguna"
        header.tvNavRole.text = when (role) {
            "cost_accounting" -> "Cost Accounting"
            "djka"            -> "Otoritas DJKA"
            "sap_erp"         -> "SAP ERP"
            else              -> "—"
        }

        if (savedInstanceState == null) {
            binding.navView.setCheckedItem(R.id.nav_dashboard)
            showFragment(DashboardFragment(), "Dashboard", addToStack = false)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.container) { v, insets ->
            v.setPadding(0, 0, 0, insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }

        // ===== BACK PRESS handler =====
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    // 1. Drawer terbuka → tutup drawer
                    binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    // 2. Ada stack fragment → pop kembali
                    fragmentStack.isNotEmpty() -> {
                        val (prev, title) = fragmentStack.removeLast()
                        supportActionBar?.title = title
                        supportFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                            .replace(R.id.container, prev)
                            .commit()
                    }
                    // 3. Di root (Dashboard) → double-back-to-exit
                    else -> {
                        val now = System.currentTimeMillis()
                        if (now - backPressedAt < 2000) {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        } else {
                            backPressedAt = now
                            Snackbar.make(
                                binding.root,
                                "Tekan sekali lagi untuk keluar",
                                Snackbar.LENGTH_SHORT
                            ).setBackgroundTint(getColor(R.color.primary))
                                .setTextColor(getColor(R.color.white))
                                .show()
                        }
                    }
                }
            }
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val (fragment, title) = fragmentFor(item.itemId)
        fragmentStack.clear()                      // reset stack saat pilih menu
        showFragment(fragment, title, addToStack = false)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun fragmentFor(itemId: Int): Pair<Fragment, String> = when (itemId) {
        R.id.nav_dashboard    -> DashboardFragment()    to "Dashboard"
        R.id.nav_kontrak      -> KontrakFragment()      to "Kontrak IMO"
        R.id.nav_biaya        -> BiayaFragment()        to "Kalkulasi Biaya"
        R.id.nav_billing      -> BillingFragment()      to "Billing & Tagihan"
        R.id.nav_aset         -> AsetFragment()         to "Aset Infrastruktur"
        R.id.nav_hak_akses    -> HakAksesFragment()     to "Hak Akses & Role"
        R.id.nav_verifikasi   -> BillingFragment()      to "Verifikasi Tagihan"
        R.id.nav_sinkronisasi -> SinkronisasiFragment() to "Sinkronisasi SAP ERP"
        R.id.nav_lainnya      -> LainnyaFragment()      to "Profil & Keluar"
        else                  -> DashboardFragment()    to "Dashboard"
    }

    /**
     * Navigasi ke fragment. addToStack=true agar back bisa kembali ke halaman sebelumnya.
     */
    fun showFragment(fragment: Fragment, title: String, addToStack: Boolean = true) {
        if (addToStack) {
            // simpan fragment saat ini ke stack
            supportFragmentManager.findFragmentById(R.id.container)?.let { current ->
                fragmentStack.addLast(current to (supportActionBar?.title?.toString() ?: ""))
            }
        }
        supportActionBar?.title = title
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_up_fade_in,
                R.anim.slide_down_fade_out,
                R.anim.slide_up_fade_in,
                R.anim.slide_down_fade_out
            )
            .replace(R.id.container, fragment)
            .commit()
    }
}
