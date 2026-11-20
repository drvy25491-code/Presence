package com.example.attendancecontrol

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class StatisticsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var allGroups = listOf<GroupEntity>()
    private var allStudents = listOf<StudentEntity>()
    private var allAttendance = listOf<AttendanceEntity>()

    private lateinit var spinnerGroup: Spinner
    private lateinit var spinnerStudent: Spinner
    private lateinit var etDate: EditText
    private lateinit var tvSummary: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var rvHistory: RecyclerView

    private var filteredStudentsForSpinner = listOf<StudentEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        db = AppDatabase.getDatabase(this)

        spinnerGroup = findViewById(R.id.spinnerStatGroup)
        spinnerStudent = findViewById(R.id.spinnerStatStudent)
        etDate = findViewById(R.id.etStatDate)
        tvSummary = findViewById(R.id.tvStatSummary)
        tvEmpty = findViewById(R.id.tvHistoryEmpty)
        rvHistory = findViewById(R.id.rvHistory)

        rvHistory.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnApplyFilter).setOnClickListener {
            applyFilters()
        }

        // Загружаем данные из Room
        lifecycleScope.launch {
            allGroups = db.groupDao().getAll()
            allStudents = db.studentDao().getAll()
            allAttendance = db.attendanceDao().getAll()

            setupGroupSpinner()
            applyFilters()
        }
    }

    private fun setupGroupSpinner() {
        val groupItems = mutableListOf("Все группы")
        groupItems.addAll(allGroups.map { it.name })

        val groupAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, groupItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(android.graphics.Color.WHITE)
                return view
            }
        }
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGroup.adapter = groupAdapter


        spinnerGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedGroup = if (position == 0) null else groupItems[position]
                setupStudentSpinner(selectedGroup)
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupStudentSpinner(groupName: String?) {
        filteredStudentsForSpinner = if (groupName == null) {
            allStudents
        } else {
            allStudents.filter { it.groupName.equals(groupName, ignoreCase = true) }
        }

        val studentItems = mutableListOf("Все студенты")
        studentItems.addAll(filteredStudentsForSpinner.map { "${it.lastName} ${it.firstName}" })

        val studentAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, studentItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(android.graphics.Color.WHITE)
                return view
            }
        }
        studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStudent.adapter = studentAdapter


        spinnerStudent.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyFilters() {
        val selectedGroupPos = spinnerGroup.selectedItemPosition
        val selectedGroup = if (selectedGroupPos > 0) spinnerGroup.selectedItem?.toString() else null

        val selectedStudentPos = spinnerStudent.selectedItemPosition
        val selectedStudent = if (selectedStudentPos > 0 && selectedStudentPos - 1 < filteredStudentsForSpinner.size) {
            filteredStudentsForSpinner[selectedStudentPos - 1]
        } else {
            null
        }

        val dateQuery = etDate.text.toString().trim()

        // Карта студентов по id для быстрого поиска ФИО и группы
        val studentMap = allStudents.associateBy { it.id }

        val records = allAttendance.filter { record ->
            val student = studentMap[record.studentId] ?: return@filter false

            val matchesGroup = selectedGroup == null || student.groupName.equals(selectedGroup, ignoreCase = true)
            val matchesStudent = selectedStudent == null || student.id == selectedStudent.id
            val matchesDate = dateQuery.isEmpty() || record.lessonDate.contains(dateQuery, ignoreCase = true)

            matchesGroup && matchesStudent && matchesDate
        }

        // Подсчёт показателей (по ориентиру Спринта 5)
        val totalLessons = records.map { it.lessonDate to it.subject }.distinct().size
        val present = records.count { it.status == "Присутствовал" }
        val absent = records.count { it.status == "Отсутствовал" }
        val late = records.count { it.status == "Опоздал" }

        tvSummary.text = "Всего занятий: $totalLessons\n" +
                "Присутствовал: $present\n" +
                "Отсутствовал: $absent\n" +
                "Опоздал: $late"

        if (records.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            rvHistory.adapter = HistoryAdapter(records, studentMap)
        }
    }
}

class HistoryAdapter(
    private val records: List<AttendanceEntity>,
    private val studentMap: Map<Int, StudentEntity>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStudent: TextView = view.findViewById(R.id.tvHistoryStudent)
        val tvDetails: TextView = view.findViewById(R.id.tvHistoryDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val record = records[position]
        val student = studentMap[record.studentId]
        val studentName = if (student != null) {
            "${student.lastName} ${student.firstName} (${student.groupName})"
        } else {
            "Удалённый студент"
        }

        holder.tvStudent.text = "$studentName — ${record.status}"
        holder.tvDetails.text = "${record.subject} | ${record.lessonDate}"
    }

    override fun getItemCount() = records.size
}
