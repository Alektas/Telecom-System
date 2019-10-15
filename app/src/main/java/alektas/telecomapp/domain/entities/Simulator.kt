package alektas.telecomapp.domain.entities

import java.util.*

class Simulator {

    companion object {
        /**
         * Частота дискретизации (количество измерений в единицу времени)
         */
        const val SAMPLING_RATE = 1.0e7

        /**
         * Период дискретизации измерения сигналов (в условных единицах времени)
         */
        const val SAMPLING_TIME = 1 / SAMPLING_RATE

        /**
         * Количество измерений за время симуляции.
         * Количество отсчетов должно быть кратно степени 2 (..., 256, 512, 1024, ...)
         * для возможности осуществления БПФ (быстрого преобразования Фурье).
         * Если требование не выполняется, то необходимо дополнять сигнал нулями до кратного
         * степени 2 размера, чтобы выполнять БПФ.
         */
        const val SAMPLE_COUNT = 8192

        /**
         * Общее время симуляции (в условных единицах времени)
         */
        const val SIMULATION_TIME = SAMPLE_COUNT * SAMPLING_TIME

        /**
         * Подсчет количества сэмплов (измерений/тактов), которое получится (измерится, пройдет)
         * за время <code>time</code>
         *
         * @return количество сэмплов (измерений/тактов)
         */
        fun samplesFor(time: Double): Int {
            return (time * SAMPLING_RATE).toInt()
        }

        /**
         * Производится вычисление функции <code>timeFunction</code> на всем временном участке
         * симуляции.
         * Общее время симуляции: {@link #SIMULATION_TIME}
         * Временные интервалы вычисления: {@link #SAMPLING_TIME}
         *
         * @param timeFunction функция с аргументом времени, по которой вычисляются значения
         *
         * @return Сортированный по времени словарь типа [время, значение]
         */
        fun simulate(timeFunction: (Double) -> Double): TreeMap<Double, Double> {
            val data = TreeMap<Double, Double>()
            for (i in 0 until SAMPLE_COUNT) {
                val time = i * SAMPLING_TIME
                data[time] = timeFunction(time)
            }
            return data
        }
    }
}