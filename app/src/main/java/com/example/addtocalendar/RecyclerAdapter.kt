package com.example.addtocalendar

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecyclerAdapter (private val data: List<DateClass>):RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val imPhoto: ImageView = itemView.findViewById(R.id.imPhoto)
        val tvEditDate: TextView = itemView.findViewById(R.id.tvEditDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.date_item, parent, false)

        val tvEditDate: TextView = itemView.findViewById(R.id.tvEditDate)
        tvEditDate.setOnClickListener {
            val date = data[tvEditDate.tag.toString().toInt()]
            val intent = Intent(parent.context, EditDateActivity::class.java)
            intent.putExtra("id", date.id)
            intent.putExtra("title", date.title)
            intent.putExtra("description", date.description)
            intent.putExtra("day", date.day)
            intent.putExtra("month", date.month)
            intent.putExtra("year", date.year)
            intent.putExtra("photo", date.photo)
            parent.context.startActivity(intent)
        }
        return ViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = data[position]
        val dateYear = if (date.year != 0) { date.year } else { "" }
        val dateDay = if (date.day != 0) { date.day } else { "" }
        val dateMonth = if (date.month != 0) { MonthAdapter.getMonthByInt(date.month) } else { "" }
        holder.tvEditDate.tag = position
        holder.tvDate.text = "$dateDay $dateMonth $dateYear".strip()
        holder.tvTitle.text = date.title
        holder.tvDescription.text = date.description
        holder.imPhoto.clipToOutline = true
        var photoUrl = "${RetrofitClient.baseUrl}${date.photo}"
        if (photoUrl.contains("\uFEFF")) {
            photoUrl = photoUrl.replace("\uFEFF", "")
        }
        Glide.with(holder.itemView).load(photoUrl).error(R.drawable.no_image).into(holder.imPhoto)
    }

    override fun getItemCount() = data.size
}