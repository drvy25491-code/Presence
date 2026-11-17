package com.example.attendancecontrol

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StudentsActivity : AppCompatActivity() {

    private lateinit var etLastName: EditText
    private lateinit var etFirstName: EditText
    private lateinit var adapter: StudentAdapter
    private val studentList = mutableListOf<Student>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_students)

        etLastName = findViewById(R.id.etLastName)
        etFirstName = findViewById(R.id.etFirstName)
        val btnAdd = findViewById<Button>(R.id.btnAddStudent)

        // ВОТ ЭТА СТРОКА ДОЛЖНА БЫТЬ БЕЗ //
        val rvStudents = findViewById<RecyclerView>(R.id.rvStudents)

        adapter = StudentAdapter(studentList) { student ->
            showDeleteConfirmation(student)
        }
        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.adapter = adapter

        btnAdd.setOnClickListener {
            val lastName = etLastName.text.toString().trim()
            val firstName = etFirstName.text.toString().trim()

            if (lastName.isEmpty() || firstName.isEmpty()) {
                Toast.makeText(this, "Заполните Фамилию и Имя!", Toast.LENGTH_SHORT).show()
            } else {
                val newStudent = Student(
                    id = (0..1000).random(),
                    firstName = firstName,
                    lastName = lastName,
                    groupName = "ИС-21"
                )
                studentList.add(newStudent)
                adapter.notifyItemInserted(studentList.size - 1)

                etLastName.text.clear()
                etFirstName.text.clear()
                Toast.makeText(this, "Студент добавлен", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation(student: Student) {
        AlertDialog.Builder(this)
            .setTitle("Удаление")
            .setMessage("Вы уверены, что хотите удалить ${student.lastName} ${student.firstName}?")
            .setPositiveButton("Удалить") { _, _ ->
                studentList.remove(student)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}

class StudentAdapter(
    private val students: List<Student>,
    private val onDeleteClick: (Student) -> Unit
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