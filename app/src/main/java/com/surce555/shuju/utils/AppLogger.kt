package com.surce555.shuju.utils

import java.text.SimpleDateFormat
import java.util.*

object AppLogger {
    private val logs = ArrayDeque<String>(500)
    private val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val listeners = mutableListOf<(String) -> Unit>()

    @Synchronized
    fun log(msg: String) {
        val entry = "[${sdf.format(Date())}] $msg"
        android.util.Log.d("ShujuApp", entry)
        logs.addLast(entry)
        if (logs.size > 500) logs.removeFirst()
        listeners.forEach { it(entry) }
    }

    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    @Synchronized
    fun getLogs(): String = logs.joinToString("\n")

    @Synchronized
    fun clear() {
        logs.clear()
    }
}
