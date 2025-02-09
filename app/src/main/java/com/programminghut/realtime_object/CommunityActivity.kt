package com.programminghut.realtime_object

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CommunityActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    // Original list of people
    val people = listOf(
        Person("John Doe", "Software Engineer", "1234567890"),
        Person("Jane Smith", "Doctor", "0987654321"),
        Person("Mike Johnson", "Designer", "1122334455"),
        Person("Alice Brown", "Teacher", "4455667788"),
        Person("Bob White", "Engineer", "6677889900")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Shuffle the list to randomize it
        val shuffledPeople = people.shuffled()

        // Set the shuffled list to the adapter
        recyclerView.adapter = PersonAdapter(this, shuffledPeople)
    }

    override fun onRestart() {
        super.onRestart()

        // Shuffle the list to randomize it
        val shuffledPeople = people.shuffled()

        // Set the shuffled list to the adapter
        recyclerView.adapter = PersonAdapter(this, shuffledPeople)
    }
}
