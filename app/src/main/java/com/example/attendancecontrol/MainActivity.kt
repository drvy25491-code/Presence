package com.example.attendancecontrol

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnStudents).setOnClickListener {
            Toast.makeText(this, "заготовка", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnAttendance).setOnClickListener {
            Toast.makeText(this, "заготовка", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnStatistics).setOnClickListener {
            Toast.makeText(this, "заготовка", Toast.LENGTH_SHORT).show()
        }

    }
}
