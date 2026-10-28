package com.example

import android.app.Application
import com.example.data.BookDatabase
import com.example.data.entity.ReadingGoalEntity
import com.example.data.repository.BookRepository
import com.example.parser.StarterBooksGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BookNestApplication : Application() {

    lateinit var database: BookDatabase
        private set

    lateinit var repository: BookRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = BookDatabase.getInstance(this)
        repository = BookRepository(
            bookDao = database.bookDao(),
            bookmarkDao = database.bookmarkDao(),
            readingSessionDao = database.readingSessionDao(),
            readingGoalDao = database.readingGoalDao()
        )

        // Seed initial starter books and default goals asynchronously if database is brand new
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure default reading goal exists
                val currentGoal = database.readingGoalDao().getGoalSync()
                if (currentGoal == null) {
                    database.readingGoalDao().insertOrUpdateGoal(
                        ReadingGoalEntity(id = 1, dailyMinutesGoal = 30, monthlyBooksGoal = 2)
                    )
                }

                // Check if any books exist
                val existingBooks = database.bookDao().getAllBooks().firstOrNull() ?: emptyList()
                if (existingBooks.isEmpty()) {
                    val starter = StarterBooksGenerator.generateStarterBooks(applicationContext)
                    database.bookDao().insertBooks(starter)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
