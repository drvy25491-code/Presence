package com.example.attendancecontrol

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AttendanceActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val currentStatuses = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)
        findViewById<Button>(R.id.btnBackToLesson).setOnClickListener {
            finish()
        }

        db = AppDatabase.getDatabase(this)

        val groupName = intent.getStringExtra("GROUP_NAME") ?: ""
        val subject = intent.getStringExtra("SUBJECT") ?: ""
        val lessonDate = intent.getStringExtra("LESSON_DATE") ?: ""

        val tvLessonInfo = findViewById<TextView>(R.id.tvLessonInfo)
        val tvEmptyList = findViewById<TextView>(R.id.tvEmptyList)
        val rvAttendance = findViewById<RecyclerView>(R.id.rvAttendance)
        val btnSave = findViewById<Button>(R.id.btnSaveAttendance)

        tvLessonInfo.text = "Группа: $groupName | $subject ($lessonDate)"

        // Загружаем данные асинхронно из Room
        lifecycleScope.launch {
            // 1. Получаем студентов нужной группы из БД
            val allStudents = db.studentDao().getAll()
            val groupStudents = allStudents.filter { it.groupName.equals(groupName, ignoreCase = true) }

            if (groupStudents.isEmpty()) {
                rvAttendance.visibility = View.GONE
                btnSave.visibility = View.GONE
                tvEmptyList.visibility = View.VISIBLE
                return@launch
            }

            // 2. Получаем существующие отметки для этого занятия
            val existingAttendance = db.attendanceDao().getByLesson(lessonDate, subject)

            // Заполняем карту статусов
            for (student in groupStudents) {
                val existing = existingAttendance.find { it.studentId == student.id }
                currentStatuses[student.id] = existing?.status ?: "Присутствовал"
            }

            // 3. Настраиваем адаптер
            val adapter = AttendanceAdapter(groupStudents, currentStatuses)
            rvAttendance.layoutManager = LinearLayoutManager(this@AttendanceActivity)
            rvAttendance.adapter = adapter

            // 4. Обработка сохранения
            btnSave.setOnClickListener {
                lifecycleScope.launch {
                    // Удаляем старые отметки за эту пару (чтобы не было дубликатов)
                    db.attendanceDao().deleteByLesson(lessonDate, subject)

                    // Вставляем новые
                    for (student in groupStudents) {
                        val status = currentStatuses[student.id] ?: "Присутствовал"
                        db.attendanceDao().insert(
                            AttendanceEntity(
                                studentId = student.id,
                                lessonDate = lessonDate,
                                subject = subject,
                                status = status
                            )
                        )
                    }
                    Toast.makeText(this@AttendanceActivity, "Посещаемость сохранена!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}

// Адаптер теперь принимает StudentEntity
class AttendanceAdapter(
    private val students: List<StudentEntity>,
    private val statusMap: MutableMap<Int, String>
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    class AttendanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvStudentNameAttendance)
        val rgStatus: RadioGroup = view.findViewById(R.id.rgStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val student = students[position]
        holder.tvName.text = "${student.lastName} ${student.firstName}"

        // Сбрасываем слушатель, чтобы не было багов при прокрутке
        holder.rgStatus.setOnCheckedChangeListener(null)

        // Устанавливаем текущий статус
        when (statusMap[student.id]) {
            "Присутствовал" -> holder.rgStatus.check(R.id.rbPresent)
            "Отсутствовал" -> holder.rgStatus.check(R.id.rbAbsent)
            "Опоздал" -> holder.rgStatus.check(R.id.rbLate)
        }

        // Обрабатываем нажатия
        holder.rgStatus.setOnCheckedChangeListener { _, checkedId ->
            val newStatus = when (checkedId) {
                R.id.rbPresent -> "Присутствовал"
                R.id.rbAbsent -> "Отсутствовал"
                R.id.rbLate -> "Опоздал"
                else -> "Присутствовал"
            }
            statusMap[student.id] = newStatus
        }
    }

    override fun getItemCount() = students.size
}