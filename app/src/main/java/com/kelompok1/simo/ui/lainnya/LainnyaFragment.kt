package com.kelompok1.simo.ui.lainnya

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kelompok1.simo.BuildConfig
import com.kelompok1.simo.data.repository.AuthRepository
import com.kelompok1.simo.databinding.FragmentLainnyaBinding
import com.kelompok1.simo.ui.auth.LoginActivity
import com.kelompok1.simo.util.SessionCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LainnyaFragment : Fragment() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var session: SessionCache

    private var _binding: FragmentLainnyaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLainnyaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvNama.text = session.namaLengkap ?: "-"
        binding.tvEmail.text = session.email ?: "-"
        binding.tvVersi.text = "SIMO v${BuildConfig.VERSION_NAME} — Prototipe Akademik"

        binding.btnLogout.setOnClickListener {
            binding.btnLogout.isEnabled = false
            lifecycleScope.launch {
                authRepository.logout()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finishAffinity()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
