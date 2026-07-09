package com.kelompok1.simo.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kelompok1.simo.R
import com.kelompok1.simo.data.repository.Aktivitas
import com.kelompok1.simo.databinding.ItemAktivitasBinding

class AktivitasAdapter :
    ListAdapter<Aktivitas, AktivitasAdapter.VH>(DIFF) {

    fun submit(list: List<Aktivitas>) = submitList(list)

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Aktivitas>() {
            override fun areItemsTheSame(a: Aktivitas, b: Aktivitas) =
                a.entitas == b.entitas && a.waktu == b.waktu
            override fun areContentsTheSame(a: Aktivitas, b: Aktivitas) = a == b
        }

        // Icon & warna bergiliran per posisi
        data class Style(val iconRes: Int, val colorRes: Int, val bgColorRes: Int)
        val STYLES = listOf(
            Style(R.drawable.ic_contract, R.color.primary, R.color.primary_50),
            Style(R.drawable.ic_cost, R.color.accent_green, R.color.accent_green_bg),
            Style(R.drawable.ic_billing, R.color.accent_amber, R.color.accent_amber_bg),
            Style(R.drawable.ic_sync, R.color.accent_purple, R.color.accent_purple_bg),
            Style(R.drawable.ic_verify, R.color.accent_teal, R.color.accent_teal_bg),
            Style(R.drawable.ic_role, R.color.accent_rose, R.color.accent_rose_bg),
        )
    }

    inner class VH(val binding: ItemAktivitasBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAktivitasBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val ctx  = holder.binding.root.context
        val style = STYLES[position % STYLES.size]

        // Teks
        holder.binding.tvJudul.text = "${item.entitas}  •  ${item.aksi}"
        holder.binding.tvWaktu.text = item.waktu

        // Icon + warna badge
        holder.binding.ivBadge.setImageResource(style.iconRes)
        holder.binding.ivBadge.imageTintList =
            ContextCompat.getColorStateList(ctx, style.colorRes)
        holder.binding.cardBadge.setCardBackgroundColor(
            ContextCompat.getColor(ctx, style.bgColorRes)
        )

        // Animasi slide-in dari kanan bertahap
        holder.binding.root.alpha = 0f
        holder.binding.root.translationX = 50f
        holder.binding.root.animate()
            .alpha(1f).translationX(0f)
            .setStartDelay((position * 60L).coerceAtMost(360L))
            .setDuration(350)
            .start()
    }
}
