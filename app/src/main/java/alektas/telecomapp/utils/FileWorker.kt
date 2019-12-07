package alektas.telecomapp.utils

import android.content.Context
import java.io.File
import java.lang.StringBuilder

class FileWorker(context: Context) {
    private val path = context.filesDir

    fun writeFile(fileName: String, data: String) {
        File(path, fileName).bufferedWriter().write(data)
    }

    fun appendToFile(fileName: String, data: String) {
        File(path, fileName).appendText(data)
    }

    fun readFile(fileName: String): String {
        val sb = StringBuilder()
        File(path, fileName).bufferedReader().useLines { it.forEach { s -> sb.append(s) } }
        return sb.toString()
    }

    fun cleanFile(fileName: String) {
        File(path, fileName).writeText("")
    }

}