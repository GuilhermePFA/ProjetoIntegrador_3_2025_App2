package com.example.pi_03_equipe_01

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    private val myList: List<HistoryItem>,
    private val onStatusClick: (String) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_history_table, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = myList[position]
        holder.idText.text = item.id
        holder.dateText.text = item.date
        holder.statusText.text = item.status.texto
        holder.statusDot.setImageResource(item.status.cor)
        holder.infoIcon.setImageResource(item.status.icone)

        holder.tableLayout.setOnClickListener {
            onStatusClick(item.id)
        }
    }

    override fun getItemCount() = myList.size

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val idText: TextView = itemView.findViewById(R.id.idText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val statusText: TextView = itemView.findViewById(R.id.statusText)
        val statusDot: ImageView = itemView.findViewById(R.id.statusDot)
        val infoIcon: ImageView = itemView.findViewById(R.id.infoIcon)
        val tableLayout: LinearLayout = itemView.findViewById((R.id.tableClick))
    }
}