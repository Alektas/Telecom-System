package alektas.telecomapp.utils

import alektas.telecomapp.BuildConfig

class L {
    companion object {
        private const val MEASURING_TIME_TAG = "MEASURING_TIME"
        private val startPoints = mutableListOf<Pair<String, Long>>()

        fun d(log: String) {
            if (BuildConfig.DEBUG) println(log)
        }

        fun d(where: Any, log: String) {
            if (BuildConfig.DEBUG) println("[${Thread.currentThread().name}][${where.javaClass.simpleName}]: $log")
        }

        fun d(tag: String, log: String) {
            if (BuildConfig.DEBUG) println("[${Thread.currentThread().name}][$tag]: $log")
        }

        /**
         * Start measuring time. Invoke this method at the moment where you want to start measuring.
         * To count time use [stop] method at right moment. It will log measured time to the Lagcat.
         */
        fun start(pointName: String = "") {
            d(MEASURING_TIME_TAG, "Start measuring from |Point-$pointName|")
            startPoints.add(pointName to System.nanoTime())
        }

        /**
         * Stop measuring time. Invoke this method at the moment where you want to stop measuring.
         * To start measuring use [start] method at right moment.
         * This method will log measured time to the Lagcat.
         */
        fun stop() {
            val endTime = System.nanoTime()
            d(MEASURING_TIME_TAG, "*** Stop measuring ***")

            startPoints.forEachIndexed { i, p ->
                d(MEASURING_TIME_TAG, "Time from |Point-$i:${p.first}| |${(endTime - p.second) * 1.0e-9}| seconds")
            }
        }
    }
}