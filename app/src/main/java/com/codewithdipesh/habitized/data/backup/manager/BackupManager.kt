package com.codewithdipesh.habitized.data.backup.manager

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.codewithdipesh.habitized.DATABASE_VERSION
import com.codewithdipesh.habitized.data.backup.model.BackupData
import com.codewithdipesh.habitized.data.backup.model.BackupFileInfo
import com.codewithdipesh.habitized.data.backup.model.BackupMetadata
import com.codewithdipesh.habitized.data.backup.model.BackupPreferences
import com.codewithdipesh.habitized.data.backup.model.GoalBackup
import com.codewithdipesh.habitized.data.backup.model.HabitBackup
import com.codewithdipesh.habitized.data.backup.model.HabitProgressBackup
import com.codewithdipesh.habitized.data.backup.model.ImageProgressBackup
import com.codewithdipesh.habitized.data.backup.model.OneTimeTaskBackup
import com.codewithdipesh.habitized.data.backup.model.SubtaskBackup
import com.codewithdipesh.habitized.data.local.dao.GoalDao
import com.codewithdipesh.habitized.data.local.dao.HabitDao
import com.codewithdipesh.habitized.data.local.dao.HabitProgressDao
import com.codewithdipesh.habitized.data.local.dao.ImageProgressDao
import com.codewithdipesh.habitized.data.local.dao.OneTimeTaskDao
import com.codewithdipesh.habitized.data.local.dao.SubTaskDao
import com.codewithdipesh.habitized.data.sharedPref.HabitPreference
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupResult {
    data class Success(val message: String, val fileName: String? = null, val jsonContent: String? = null) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

@Singleton
class BackupManager @Inject constructor(
    private val context: Context,
    private val habitDao: HabitDao,
    private val habitProgressDao: HabitProgressDao,
    private val goalDao: GoalDao,
    private val subtaskDao: SubTaskDao,
    private val imageProgressDao: ImageProgressDao,
    private val oneTimeTaskDao: OneTimeTaskDao,
    private val imageBackupManager: ImageBackupManager,
    private val habitPreference: HabitPreference
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val backupDir: File
        get() = File(context.filesDir, "backups").also { it.mkdirs() }

    companion object {
        private const val BACKUP_FILE_PREFIX = "habitized_backup_"
        private const val BACKUP_FILE_EXTENSION = ".json"
    }

    suspend fun createBackup(backupType: String = "manual"): BackupResult = withContext(Dispatchers.IO) {
        when (val result = prepareBackupJson(backupType)) {
            is BackupResult.Error -> result
            is BackupResult.Success -> {
                val fileName = result.fileName ?: return@withContext BackupResult.Error("Backup failed")
                val jsonContent = result.jsonContent ?: return@withContext BackupResult.Error("Backup failed")
                try {
                    val file = File(backupDir, fileName)
                    file.writeText(jsonContent)
                    BackupResult.Success("Backup created successfully", fileName)
                } catch (e: Exception) {
                    BackupResult.Error("Failed to save backup file")
                }
            }
        }
    }

    suspend fun prepareBackupJson(backupType: String = "manual"): BackupResult = withContext(Dispatchers.IO) {
        try {
            val habits = habitDao.getAllHabits().map { HabitBackup.fromEntity(it) }
            val habitProgress = habitProgressDao.getAllProgress().map { HabitProgressBackup.fromEntity(it) }
            val goals = goalDao.getAllGoals().map { GoalBackup.fromEntity(it) }
            val subtasks = subtaskDao.getAllSubtasks().map { SubtaskBackup.fromEntity(it) }
            val imageProgress = imageProgressDao.getAllImageProgress().map { ImageProgressBackup.fromEntity(it) }
            val oneTimeTasks = oneTimeTaskDao.getAllTasks().map { OneTimeTaskBackup.fromEntity(it) }

            val images = imageBackupManager.encodeAllImages(imageProgress)

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            val metadata = BackupMetadata(
                appVersion = versionName,
                appVersionCode = versionCode,
                databaseVersion = DATABASE_VERSION,
                backupTimestamp = timestamp,
                backupType = backupType,
                deviceModel = Build.MODEL,
                androidVersion = Build.VERSION.SDK_INT
            )

            val preferences = BackupPreferences(
                theme = habitPreference.getTheme(),
                introShown = !habitPreference.isOnboardingRequired(),
                autoBackupEnabled = habitPreference.isAutoBackupEnabled()
            )

            val backupData = BackupData(
                metadata = metadata,
                habits = habits,
                habitProgress = habitProgress,
                goals = goals,
                subtasks = subtasks,
                imageProgress = imageProgress,
                oneTimeTasks = oneTimeTasks,
                preferences = preferences,
                images = images
            )

            val jsonContent = gson.toJson(backupData)
            val fileName = generateFileName()

            habitPreference.updateLastBackupDate(timestamp)
            BackupResult.Success("Backup prepared", fileName, jsonContent)
        } catch (e: Exception) {
            BackupResult.Error("Backup failed: ${e.message}")
        }
    }

    suspend fun saveToUri(uri: Uri, content: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            } ?: return@withContext BackupResult.Error("Could not open file")
            BackupResult.Success("Backup saved successfully")
        } catch (e: Exception) {
            BackupResult.Error("Failed to save: ${e.message}")
        }
    }

    suspend fun restoreBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Read backup file
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val jsonContent = inputStream?.bufferedReader()?.use { it.readText() }
                ?: return@withContext BackupResult.Error("Could not read backup file")

            // Parse backup data
            val backupData = try {
                gson.fromJson(jsonContent, BackupData::class.java)
            } catch (e: Exception) {
                return@withContext BackupResult.Error("Invalid backup file format")
            }

            // Validate backup
            val validationResult = validateBackup(backupData)
            if (validationResult != null) {
                return@withContext BackupResult.Error(validationResult)
            }

            // Clear existing data
            clearAllData()

            // Restore in correct order for foreign key integrity
            // 1. Goals (no dependencies)
            backupData.goals.forEach { goal ->
                goalDao.insertGoal(goal.toEntity())
            }

            // 2. Habits (depends on goals)
            backupData.habits.forEach { habit ->
                habitDao.insertHabit(habit.toEntity())
            }

            // 3. HabitProgress (depends on habits)
            backupData.habitProgress.forEach { progress ->
                habitProgressDao.insertProgress(progress.toEntity())
            }

            // 4. Subtasks (depends on habitProgress)
            backupData.subtasks.forEach { subtask ->
                subtaskDao.insertSubtask(subtask.toEntity())
            }

            // 5. ImageProgress (depends on habits)
            backupData.imageProgress.forEach { imageProgress ->
                imageProgressDao.insert(imageProgress.toEntity())
            }

            // 6. OneTimeTasks (independent)
            backupData.oneTimeTasks.forEach { task ->
                oneTimeTaskDao.insertTask(task.toEntity())
            }

            // 7. Restore images
            imageBackupManager.restoreImages(backupData.images)

            // 8. Restore preferences
            habitPreference.updateTheme(backupData.preferences.theme)
            habitPreference.setOnboardingRequired(!backupData.preferences.introShown)
            habitPreference.setAutoBackupEnabled(backupData.preferences.autoBackupEnabled)

            BackupResult.Success("Restore completed successfully")
        } catch (e: Exception) {
            BackupResult.Error("Restore failed: ${e.message}")
        }
    }


    suspend fun listAvailableBackups(): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        backupDir.listFiles { file ->
            file.name.startsWith(BACKUP_FILE_PREFIX) &&
                    file.name.endsWith(BACKUP_FILE_EXTENSION)
        }?.sortedByDescending { it.lastModified() }?.map { file ->
            val dateTime = java.text.SimpleDateFormat(
                "MMM dd, yyyy HH:mm",
                java.util.Locale.getDefault()
            ).format(java.util.Date(file.lastModified()))

            BackupFileInfo(
                fileName = file.name,
                displayName = file.name.removeSuffix(BACKUP_FILE_EXTENSION)
                    .removePrefix(BACKUP_FILE_PREFIX),
                dateTime = dateTime,
                fileSizeBytes = file.length(),
                fileSizeDisplay = formatFileSize(file.length()),
                uri = Uri.fromFile(file).toString()
            )
        } ?: emptyList()
    }

    suspend fun deleteBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val deleted = if (uri.scheme == "file") {
                File(uri.path!!).delete()
            } else {
                context.contentResolver.delete(uri, null, null) > 0
            }
            if (deleted) {
                BackupResult.Success("Backup deleted successfully")
            } else {
                BackupResult.Error("Could not delete backup file")
            }
        } catch (e: Exception) {
            BackupResult.Error("Delete failed: ${e.message}")
        }
    }


    private fun validateBackup(backupData: BackupData): String? {
        // Check metadata
        if (backupData.metadata.appVersion.isEmpty()) {
            return "Invalid backup: missing app version"
        }

        // Check database version compatibility
        if (backupData.metadata.databaseVersion > DATABASE_VERSION) {
            return "Backup was created with a newer version of the app. Please update the app first."
        }

        return null
    }


    private suspend fun clearAllData() {
        // Delete in reverse order of dependencies
        subtaskDao.deleteAllSubtasks()
        imageProgressDao.deleteAll()
        habitProgressDao.deleteAllProgress()
        habitDao.deleteAllHabits()
        goalDao.deleteAllGoals()
        oneTimeTaskDao.deleteAllTasks()

        // Clear existing images
        imageBackupManager.clearExistingImages()
    }


    private fun generateFileName(): String {
        val timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        )
        return "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_EXTENSION"
    }

    private fun saveToDownloads(fileName: String, content: String): Boolean {
        return try {
            val file = File(backupDir, fileName)
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }


    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }


    suspend fun getBackupSummary(uri: Uri): Map<String, Int>? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonContent = inputStream?.bufferedReader()?.use { it.readText() } ?: return@withContext null
            val backupData = gson.fromJson(jsonContent, BackupData::class.java)

            mapOf(
                "habits" to backupData.habits.size,
                "progress" to backupData.habitProgress.size,
                "goals" to backupData.goals.size,
                "tasks" to backupData.oneTimeTasks.size,
                "images" to backupData.images.size
            )
        } catch (e: Exception) {
            null
        }
    }
}
