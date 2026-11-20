package com.example.attendancecontrol

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class StudentsActivity : AppCompatActivity() {

    private lateinit var etLastName: EditText
    private lateinit var etFirstName: EditText
    private lateinit var spinnerGroups: Spinner
    private lateinit var adapter: StudentAdapter

    private val studentList = mutableListOf<StudentEntity>()

    private lateinit var db: AppDatabase
    private lateinit var studentDao: StudentDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_students)
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        db = AppDatabase.getDatabase(this)
        studentDao = db.studentDao()

        etLastName = findViewById(R.id.etLastName)
        etFirstName = findViewById(R.id.etFirstName)
        spinnerGroups = findViewById(R.id.spinnerGroups)
        val btnAdd = findViewById<Button>(R.id.btnAddStudent)
        val rvStudents = findViewById<RecyclerView>(R.id.rvStudents)

        // Загружаем список групп для Spinner из Room
        lifecycleScope.launch {
            val groups = db.groupDao().getAll()
            val groupNames = groups.map { it.name }
            val spinnerAdapter = ArrayAdapter(
                this@StudentsActivity,
                android.R.layout.simple_spinner_item,
                groupNames
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGroups.adapter = spinnerAdapter
        }

        adapter = StudentAdapter(studentList) { student ->
            showDeleteConfirmation(student)
        }
        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.adapter = adapter

        loadStudents()

        btnAdd.setOnClickListener {
            val lastName = etLastName.text.toString().trim()
            val firstName = etFirstName.text.toString().trim()
            val selectedGroup = spinnerGroups.selectedItem?.toString() ?: ""

            if (selectedGroup.isEmpty()) {
                Toast.makeText(this, "Сначала создайте хотя бы одну группу!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (lastName.isEmpty() || firstName.isEmpty()) {
                Toast.makeText(this, "Заполните Фамилию и Имя!", Toast.LENGTH_SHORT).show()
            } else {
                val newStudent = StudentEntity(
                    firstName = firstName,
                    lastName = lastName,
                    groupName = selectedGroup
                )
                lifecycleScope.launch {
                    studentDao.insert(newStudent)
                    loadStudents()
                    etLastName.text.clear()
                    etFirstName.text.clear()
                    Toast.makeText(this@StudentsActivity, "Студент добавлен", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadStudents() {
        lifecycleScope.launch {
            val fromDb = studentDao.getAll()
            studentList.clear()
            studentList.addAll(fromDb)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showDeleteConfirmation(student: StudentEntity) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Удаление")
            .setMessage("Вы уверены, что хотите удалить ${student.lastName} ${student.firstName}?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    studentDao.delete(student)
                    loadStudents()
                    Toast.makeText(this@StudentsActivity, "Студент удален", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#E53935"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#757575"))
    }
}

class StudentAdapter(
    private val students: List<StudentEntity>,
    private val onDeleteClick: (StudentEntity) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvStudentName)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.tvName.text = "${student.lastName} ${student.firstName} (${student.groupName})"
        holder.btnDelete.setOnClickListener {
            onDeleteClick(student)
        }
    }

    override fun getItemCount() = students.size
}
