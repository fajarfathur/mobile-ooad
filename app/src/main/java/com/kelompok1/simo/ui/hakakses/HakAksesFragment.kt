package com.kelompok1.simo.ui.hakakses

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.kelompok1.simo.databinding.FragmentHakAksesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HakAksesFragment : Fragment() {

    private var _binding: FragmentHakAksesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HakAksesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHakAksesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTambahUser.setOnClickListener { showCreateDialog() }
        binding.btnKelolRole.setOnClickListener { showManageUserChooser() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ui.collect(::render)
        }
    }

    private fun render(state: HakAksesUiState) {
        binding.tvUserList.text = when {
            state.loading && state.users.isEmpty() -> "Memuat data dari Supabase…"
            state.users.isEmpty() -> "Belum ada pengguna yang terdaftar."
            else -> state.users.joinToString("\n\n") { user ->
                val icon = if (user.isActive) "🟢" else "🔴"
                val roleLabel = state.roles.firstOrNull { it.id == user.roleId }?.label ?: user.roleNama
                "$icon ${user.nama}\n  Role: $roleLabel | Unit: ${user.unit ?: "-"} | Status: ${if (user.isActive) "Aktif" else "Nonaktif"}"
            }
        }

        binding.tvRoleList.text = when {
            state.loading && state.roles.isEmpty() -> "Memuat data dari Supabase…"
            state.roles.isEmpty() -> "Role tidak ditemukan."
            else -> state.roles.joinToString("\n\n") { role ->
                "• ${role.nama.padEnd(16, ' ')} — ${role.deskripsi ?: role.label}"
            }
        }

        val status = state.error ?: state.message
        binding.cardStatusAksi.isVisible = status != null
        binding.tvStatusAksi.text = status.orEmpty()
    }

    private fun showCreateDialog() {
        val state = viewModel.ui.value
        if (state.roles.isEmpty()) {
            binding.cardStatusAksi.isVisible = true
            binding.tvStatusAksi.text = "Role belum tersedia. Muat ulang data terlebih dahulu."
            return
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val etNama = EditText(requireContext()).apply { hint = "Nama lengkap" }
        val etEmail = EditText(requireContext()).apply {
            hint = "Email login"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val etPassword = EditText(requireContext()).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etUnit = EditText(requireContext()).apply { hint = "Unit kerja" }
        val spinnerRole = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                state.roles.map { it.label }
            )
        }

        listOf(etNama, etEmail, etPassword, etUnit, spinnerRole).forEach {
            container.addView(it)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah User SIMO")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val role = state.roles[spinnerRole.selectedItemPosition]
                viewModel.createUser(
                    nama = etNama.text.toString(),
                    email = etEmail.text.toString(),
                    password = etPassword.text.toString(),
                    roleId = role.id,
                    unit = etUnit.text.toString()
                )
            }
            .show()
    }

    private fun showManageUserChooser() {
        val users = viewModel.ui.value.users
        if (users.isEmpty()) {
            binding.cardStatusAksi.isVisible = true
            binding.tvStatusAksi.text = "Belum ada user untuk dikelola."
            return
        }

        val labels = users.map { user ->
            "${user.nama} • ${if (user.isActive) "Aktif" else "Nonaktif"}"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih User")
            .setItems(labels) { _, which ->
                showManageDialog(users[which])
            }
            .show()
    }

    private fun showManageDialog(user: com.kelompok1.simo.data.repository.AccessUser) {
        val state = viewModel.ui.value
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val etUnit = EditText(requireContext()).apply {
            hint = "Unit kerja"
            setText(user.unit.orEmpty())
        }
        val cbActive = CheckBox(requireContext()).apply {
            text = "User aktif"
            isChecked = user.isActive
        }
        val spinnerRole = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                state.roles.map { it.label }
            )
            setSelection(state.roles.indexOfFirst { it.id == user.roleId }.coerceAtLeast(0))
        }

        listOf(spinnerRole, etUnit, cbActive).forEach { container.addView(it) }

        AlertDialog.Builder(requireContext())
            .setTitle("Kelola ${user.nama}")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val selectedRole = state.roles[spinnerRole.selectedItemPosition]
                viewModel.updateUser(
                    user = user,
                    roleId = selectedRole.id,
                    isActive = cbActive.isChecked,
                    unit = etUnit.text.toString()
                )
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
