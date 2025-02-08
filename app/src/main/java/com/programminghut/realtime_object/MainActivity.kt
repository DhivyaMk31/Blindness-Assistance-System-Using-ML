package com.programminghut.realtime_object

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val liveObjectDetectionButton = findViewById<Button>(R.id.button_live_object_detection)
        val imageToTextButton = findViewById<Button>(R.id.button_image_to_text)

        liveObjectDetectionButton.setOnClickListener {
            startActivity(Intent(this, LiveObjectDetectionActivity::class.java))
        }

        imageToTextButton.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }
}
