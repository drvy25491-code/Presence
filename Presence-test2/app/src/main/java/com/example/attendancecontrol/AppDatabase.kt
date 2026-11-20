package com.example.attendancecontrol

import android.content.Context
import androidx.room.*

// 1. ТАБЛИЦА ГРУПП
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

// 2. ТАБЛИЦА СТУДЕНТОВ
@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firstName: String,
    val lastName: String,
    val groupName: String
)

// 3. ТАБЛИЦА ЗАНЯТИЙ (Добавлено)
@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupName: String,
    val lessonDate: String,
    val subject: String
)

// 4. ТАБЛИЦА ПОСЕЩАЕМОСТИ
@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val lessonDate: String,
    val subject: String,
    val status: String
)

// 5. DAO ДЛЯ ГРУПП
@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)

    @Query("SELECT * FROM `groups` ORDER BY name")
    suspend fun getAll(): List<GroupEntity>

    @Delete
    suspend fun delete(group: GroupEntity)
}

// 6. DAO ДЛЯ СТУДЕНТОВ
@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(student: StudentEntity)

    @Query("SELECT * FROM students ORDER BY lastName")
    suspend fun getAll(): List<StudentEntity>

    @Delete
    suspend fun delete(student: StudentEntity)
}

// 7. DAO ДЛЯ ЗАНЯТИЙ (Добавлено)
@Dao
interface LessonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lesson: LessonEntity)

    @Query("SELECT * FROM lessons ORDER BY id DESC")
    suspend fun getAll(): List<LessonEntity>

    @Delete
    suspend fun delete(lesson: LessonEntity)
}

// 8. DAO ДЛЯ ОТМЕТОК
@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance WHERE lessonDate = :date AND subject = :subject")
    suspend fun getByLesson(date: String, subject: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance ORDER BY id DESC")
    suspend fun getAll(): List<AttendanceEntity>

    @Query("DELETE FROM attendance WHERE lessonDate = :date AND subject = :subject")
    suspend fun deleteByLesson(date: String, subject: String)
}


// 9. САМА БАЗА ДАННЫХ (Добавлена LessonEntity, version = 3)
@Database(
    entities = [GroupEntity::class, StudentEntity::class, LessonEntity::class, AttendanceEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun studentDao(): StudentDao
    abstract fun lessonDao(): LessonDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
