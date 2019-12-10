package alektas.telecomapp.utils

import alektas.telecomapp.BuildConfig

class L {
    companion object {
        fun d(log: String) {
            if (BuildConfig.DEBUG) println(log)
        }

        fun d(where: Any, log: String) {
            if (BuildConfig.DEBUG) println("[${where.javaClass.simpleName}]: $log")
        }

        fun d(tag: String, log: String) {
            if (BuildConfig.DEBUG) println("[$tag]: $log")
        }
    }
}