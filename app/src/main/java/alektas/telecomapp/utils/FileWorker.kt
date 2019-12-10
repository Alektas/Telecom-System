package alektas.telecomapp.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class FileWorker(private val context: Context) {
    private val path = context.filesDir

    fun createFile(fileName: String, data: String) {
        File(path, fileName).bufferedWriter().use { it.write(data) }
    }

    fun appendToFile(fileName: String, data: String) {
        val f = File(path, fileName)
        FileOutputStream(f, true).bufferedWriter().use { it.write(data) }
    }

    fun readFile(fileName: String): String {
        return File(path, fileName).bufferedReader().use { it.readText() }
    }

    fun readFile(uri: Uri): String {
        val inStream = context.contentResolver.openInputStream(uri)
        return inStream?.bufferedReader()?.use { it.readText() } ?: ""
    }

    fun cleanFile(fileName: String) {
        File(path, fileName).writeText("")
    }

}