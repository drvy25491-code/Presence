package com.example.attendancecontrol

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LessonSetupActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var lessonDao: LessonDao
    private lateinit var adapter: LessonAdapter
    private val lessonList = mutableListOf<LessonEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_setup)

        db = AppDatabase.getDatabase(this)
        lessonDao = db.lessonDao()

        val spinnerGroup = findViewById<Spinner>(R.id.spinnerLessonGroup)
        val etSubject = findViewById<EditText>(R.id.etSubject)
        val etDate = findViewById<EditText>(R.id.etDate)
        val btnStartLesson = findViewById<Button>(R.id.btnStartLesson)
        val rvLessons = findViewById<RecyclerView>(R.id.rvLessons)
        val btnBack = findViewById<Button>(R.id.btnBack)

        etDate.setText(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))

        // Настройка списка созданных занятий
        adapter = LessonAdapter(
            lessons = lessonList,
            onItemClick = { lesson ->
                openAttendance(lesson.groupName, lesson.subject, lesson.lessonDate)
            },
            onDeleteClick = { lesson ->
                showDeleteConfirmation(lesson)
            }
        )
        rvLessons.layoutManager = LinearLayoutManager(this)
        rvLessons.adapter = adapter

        // Загружаем группы в Spinner
        lifecycleScope.launch {
            val groups = db.groupDao().getAll()
            val groupNames = groups.map { it.name }

            val spinnerAdapter = ArrayAdapter(
                this@LessonSetupActivity,
                android.R.layout.simple_spinner_item,
                groupNames
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGroup.adapter = spinnerAdapter
        }

        // Загружаем сохранённые занятия
        loadLessons()

        btnStartLesson.setOnClickListener {
            val group = spinnerGroup.selectedItem?.toString() ?: ""
            val subject = etSubject.text.toString().trim()
            val date = etDate.text.toString().trim()

            if (group.isEmpty()) {
                Toast.makeText(this, "Сначала создайте группу!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (subject.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Заполните предмет и дату!", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    // Сохраняем занятие в БД, если такого ещё нет
                    val exists = lessonList.any {
                        it.groupName.equals(group, ignoreCase = true) &&
                                it.subject.equals(subject, ignoreCase = true) &&
                                it.lessonDate == date
                    }
                    if (!exists) {
                        lessonDao.insert(
                            LessonEntity(
                                groupName = group,
                                lessonDate = date,
                                subject = subject
                            )
                        )
                        loadLessons()
                    }
                    openAttendance(group, subject, date)
                }
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun openAttendance(group: String, subject: String, date: String) {
        val intent = Intent(this, AttendanceActivity::class.java).apply {
            putExtra("GROUP_NAME", group)
            putExtra("SUBJECT", subject)
            putExtra("LESSON_DATE", date)
        }
        startActivity(intent)
    }

    private fun loadLessons() {
        lifecycleScope.launch {
            val fromDb = lessonDao.getAll()
            lessonList.clear()
            lessonList.addAll(fromDb)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showDeleteConfirmation(lesson: LessonEntity) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Удаление занятия")
            .setMessage("Удалить занятие \"${lesson.subject}\" (${lesson.groupName}, ${lesson.lessonDate})?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    lessonDao.delete(lesson)
                    db.attendanceDao().deleteByLesson(lesson.lessonDate, lesson.subject)
                    loadLessons()
                    Toast.makeText(this@LessonSetupActivity, "Занятие удалено", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#E53935"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#757575"))
    }
}

class LessonAdapter(
    private val lessons: List<LessonEntity>,
    private val onItemClick: (LessonEntity) -> Unit,
    private val onDeleteClick: (LessonEntity) -> Unit
) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    class LessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInfo: TextView = view.findViewById(R.id.tvLessonInfo)
        val btnDelete: Button = view.findViewById(R.id.btnDeleteLesson)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]
        holder.tvInfo.text = "${lesson.subject}\nГруппа: ${lesson.groupName} | ${lesson.lessonDate}"
        holder.itemView.setOnClickListener {
            onItemClick(lesson)
        }
        holder.btnDelete.setOnClickListener {
            onDeleteClick(lesson)
        }
    }

    override fun getItemCount() = lessons.size
}
