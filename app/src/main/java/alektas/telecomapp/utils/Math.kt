package alektas.telecomapp.utils

class Math {
    companion object {

        /**
         * Integrate function by the trapezoidal rule for the one interval.
         * This function is low accurate. If you need more accuracy use other [integrate] function instead.
         */
        fun integrate(from: Double, to: Double, function: (Double) -> Double): Double {
            return (to - from) * ( 0.5 * (function(from) + function(to)) )
        }

        /**
         * Integrate function by the trapezoidal rule on the specified number of intervals.
         */
        fun integrate(from: Double, to: Double, intervals: Int, function: (Double) -> Double): Double {
            val step = (to - from) / intervals
            var result = 0.0
            for (i in 0 until intervals) {
                val start = from + step * i
                result += integrate(start, start + step, function)
            }
            return result
        }
    }
}