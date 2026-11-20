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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class GroupsActivity : AppCompatActivity() {

    private lateinit var adapter: GroupAdapter
    private val groupList = mutableListOf<GroupEntity>()
    private lateinit var db: AppDatabase
    private lateinit var groupDao: GroupDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        db = AppDatabase.getDatabase(this)
        groupDao = db.groupDao()

        val etGroupName = findViewById<EditText>(R.id.etGroupName)
        val btnCreateGroup = findViewById<Button>(R.id.btnCreateGroup)
        val rvGroups = findViewById<RecyclerView>(R.id.rvGroups)

        adapter = GroupAdapter(groupList) { group ->
            showDeleteConfirmation(group)
        }
        rvGroups.layoutManager = LinearLayoutManager(this)
        rvGroups.adapter = adapter

        loadGroups()

        btnCreateGroup.setOnClickListener {
            val name = etGroupName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Введите название группы", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (groupList.any { it.name.equals(name, ignoreCase = true) }) {
                Toast.makeText(this, "Такая группа уже существует!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                groupDao.insert(GroupEntity(name = name))
                loadGroups()
                etGroupName.text.clear()
                Toast.makeText(this@GroupsActivity, "Группа добавлена", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadGroups() {
        lifecycleScope.launch {
            val fromDb = groupDao.getAll()
            groupList.clear()
            groupList.addAll(fromDb)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showDeleteConfirmation(group: GroupEntity) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Удаление группы")
            .setMessage("Вы уверены, что хотите удалить группу \"${group.name}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    groupDao.delete(group)
                    loadGroups()
                    Toast.makeText(this@GroupsActivity, "Группа удалена", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#E53935"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#757575"))
    }
}

class GroupAdapter(
    private val groups: List<GroupEntity>,
    private val onDeleteClick: (GroupEntity) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvGroupName)
        val btnDelete: Button = view.findViewById(R.id.btnDeleteGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.tvName.text = group.name
        holder.btnDelete.setOnClickListener {
            onDeleteClick(group)
        }
    }

    override fun getItemCount() = groups.size
}
