package com.pardeep.bundlepassing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MyAdp(
    var studentArray: ArrayList<StudentData>,
    var recyclerInterface: recyclerInterface,
    var context: Fragment
) : RecyclerView.Adapter<MyAdp.ViewHolder>() {
    class ViewHolder(var view : View) : RecyclerView.ViewHolder(view) {
        var studentName : TextView = view.findViewById(R.id.studentName)
        var studentAge : TextView = view.findViewById(R.id.studentAge)
        var deleteBtn : Button = view.findViewById(R.id.deleteBtn)
        var updateBtn : Button = view.findViewById(R.id.updateBtn)
        var cardView : CardView = view.findViewById(R.id.customRecCardView)
        var studentImage : ImageView = view.findViewById(R.id.rImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.custom_layout,parent,false))
    }

    override fun getItemCount(): Int {
        return studentArray.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.studentName.setText(studentArray[position].name)
        Glide.with(context)
            .load(studentArray[position].image)
            .into(holder.studentImage)
        holder.studentAge.setText(studentArray[position].age.toString())
        holder.cardView.setOnClickListener {
            recyclerInterface.onItemClick(position)
        }
        holder.deleteBtn.setOnClickListener {
            recyclerInterface.operationClick(position,"delete")
        }
        holder.updateBtn.setOnClickListener {
            recyclerInterface.operationClick(position,"update")

        }
    }

}
