package com.kelompok1.simo.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kelompok1.simo.data.repository.Aktivitas
import com.kelompok1.simo.databinding.ItemAktivitasBinding

class AktivitasAdapter(
    private var items: List<Aktivitas> = emptyList()
) : RecyclerView.Adapter<AktivitasAdapter.VH>() {

    fun submit(list: List<Aktivitas>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemAktivitasBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemAktivitasBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = items[position]
        holder.binding.tvJudul.text = "${a.entitas} • ${a.aksi}"
        holder.binding.tvWaktu.text = a.waktu
    }

    override fun getItemCount() = items.size
}
