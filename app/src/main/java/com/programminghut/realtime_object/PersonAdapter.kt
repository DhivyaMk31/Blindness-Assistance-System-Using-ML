package com.programminghut.realtime_object

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PersonAdapter(private val context: Context, private val people: List<Person>) :
    RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

    class PersonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val detailsTextView: TextView = view.findViewById(R.id.detailsTextView)
        val phoneTextView: TextView = view.findViewById(R.id.phoneTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_person, parent, false)
        return PersonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val person = people[position]
        holder.nameTextView.text = person.name
        holder.detailsTextView.text = person.details
        holder.phoneTextView.text = person.phoneNumber

        // Call on click
        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${person.phoneNumber}")
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = people.size
}
