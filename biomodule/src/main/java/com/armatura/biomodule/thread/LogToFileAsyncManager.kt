package com.armatura.biomodule.thread

import android.os.HandlerThread
import android.util.Log
import com.armatura.biomodule.activity.base.ExApplication
import java.io.File
import java.io.FileWriter
import java.io.IOException

object LogToFileAsyncManager {
    private const val TAG = "LogToFileAsyncManager"
    private const val MAX_LOG_FILE_SIZE: Int = 1024 * 1024 * 1024

    private val logFileThread by lazy {
        HandlerThread("FileLogThread").apply {
            start()
        }
    }
    private val logFileHandler by lazy {
        android.os.Handler(logFileThread.looper)
    }

    private fun getLogFileDir(): File {
        return ExApplication.instance().externalCacheDir ?: ExApplication.instance().cacheDir
    }


    fun saveRecord(fileName: String, str: String) {
        logFileHandler.post {
            writeFile(fileName, str)
        }
    }

    private fun writeFile(fileName: String, str: String) {
        val record = File(getLogFileDir(), fileName)
        try {
            if (!record.exists()) {
                val dir = getLogFileDir()
                dir.mkdirs()
                record.createNewFile()
            } else {
                if (record.length() > MAX_LOG_FILE_SIZE) {
                    if (record.delete()) {
                        record.createNewFile()
                    }
                }
            }
            var writer: FileWriter? = null
            try {
                writer = FileWriter(record, true)
                writer.write(str + "\n")
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    writer?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "" + e.message)
        }
    }
}