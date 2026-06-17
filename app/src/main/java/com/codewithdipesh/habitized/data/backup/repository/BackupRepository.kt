package com.codewithdipesh.habitized.data.backup.repository

import android.net.Uri
import com.codewithdipesh.habitized.data.backup.manager.BackupResult
import com.codewithdipesh.habitized.data.backup.model.BackupFileInfo

interface BackupRepository {
    suspend fun createBackup(backupType: String = "manual"): BackupResult
    suspend fun prepareBackup(backupType: String = "manual"): BackupResult
    suspend fun saveToUri(uri: Uri, content: String): BackupResult
    suspend fun restoreBackup(uri: Uri): BackupResult
    suspend fun listBackups(): List<BackupFileInfo>
    suspend fun deleteBackup(uri: Uri): BackupResult
    suspend fun getBackupSummary(uri: Uri): Map<String, Int>?
    fun getLastBackupDate(): String?
    fun isAutoBackupEnabled(): Boolean
    fun setAutoBackupEnabled(enabled: Boolean)
}
