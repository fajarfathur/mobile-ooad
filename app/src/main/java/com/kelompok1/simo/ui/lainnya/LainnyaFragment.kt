package com.kelompok1.simo.ui.lainnya

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.kelompok1.simo.R
import com.kelompok1.simo.databinding.FragmentLainnyaBinding
import com.kelompok1.simo.data.repository.AuthRepository
import com.kelompok1.simo.ui.auth.LoginActivity
import com.kelompok1.simo.util.SessionCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class LainnyaFragment : Fragment() {

    @Inject lateinit var session: SessionCache
    @Inject lateinit var authRepository: AuthRepository

    private var _binding: FragmentLainnyaBinding? = null
    private val binding get() = _binding!!

    /** File path foto profil lokal */
    private val avatarFile by lazy {
        File(requireContext().filesDir, "avatar_${session.userId ?: "user"}.jpg")
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { saveAndDisplayAvatar(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLainnyaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // === Isi data profil ===
        binding.tvProfileNama.text  = session.namaLengkap ?: "—"
        binding.tvProfileEmail.text = session.email       ?: "—"
        binding.tvProfileRole.text  = when (session.roleName) {
            "cost_accounting" -> "Cost Accounting"
            "djka"            -> "Otoritas DJKA"
            "sap_erp"         -> "SAP ERP (Integrasi)"
            else              -> "—"
        }
        binding.tvProfileUnit.text    = session.unit ?: "PT Kereta Api Indonesia"
        binding.tvSistemVersion.text  = "SIMO v1.0 — Prototipe Akademik"
        binding.tvSistemSupabase.text = "Supabase PostgreSQL ✓ Terhubung"

        // Kredensial demo — tampilkan di card info
        val cred = when (session.email) {
            "ca@kai.id"   -> "ca@kai.id  |  demo123"
            "djka@kai.id" -> "djka@kai.id  |  demo123"
            "sap@kai.id"  -> "sap@kai.id  |  demo123"
            else          -> "${session.email}  |  demo123"
        }
        binding.tvCredentials.text = "Akun aktif: $cred"

        // === Load avatar yang sudah tersimpan ===
        loadSavedAvatar()

        // === Edit foto ===
        binding.btnEditPhoto.setOnClickListener { pickImage.launch("image/*") }
        binding.cardAvatar.setOnClickListener   { pickImage.launch("image/*") }

        // === Logout ===
        binding.btnLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                authRepository.logout()
                startActivity(
                    Intent(requireContext(), LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
        }
    }

    /** Simpan foto ke file lokal lalu tampilkan */
    private fun saveAndDisplayAvatar(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                // Simpan ke file lokal
                FileOutputStream(avatarFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                applyBitmapToAvatar(bmp)
                Snackbar.make(binding.root, "✓ Foto profil diperbarui", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(resources.getColor(R.color.primary, null))
                    .setTextColor(resources.getColor(R.color.white, null))
                    .show()
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Gagal memuat foto: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }

    /** Load avatar dari file lokal (persists antar sesi) */
    private fun loadSavedAvatar() {
        if (avatarFile.exists()) {
            try {
                val bmp = BitmapFactory.decodeFile(avatarFile.absolutePath)
                if (bmp != null) applyBitmapToAvatar(bmp)
            } catch (_: Exception) {}
        }
    }

    private fun applyBitmapToAvatar(bmp: Bitmap) {
        binding.ivAvatar.setImageBitmap(bmp)
        binding.ivAvatar.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.ivAvatar.setPadding(0, 0, 0, 0)
        binding.ivAvatar.imageTintList = null   // hapus tint agar foto terlihat asli
        binding.ivAvatar.background = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
