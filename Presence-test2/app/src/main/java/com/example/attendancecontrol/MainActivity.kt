package com.example.attendancecontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnStudents).setOnClickListener {
            val intent = Intent(this, StudentsActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnAttendance).setOnClickListener {
            // Теперь мы открываем экран настройки занятия
            startActivity(Intent(this, LessonSetupActivity::class.java))
        }
        findViewById<Button>(R.id.btnAddGroup).setOnClickListener {
            val intent = Intent(this, GroupsActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnStatistics).setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

    }
}