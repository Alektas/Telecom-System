package alektas.telecomapp.domain.entities

import java.util.*
import kotlin.math.ceil

class Simulator {

    companion object {
        /**
         * Частота дискретизации (количество измерений в единицу времени)
         */
        const val DEFAULT_SAMPLING_RATE = 1.0e7
        var samplingRate = DEFAULT_SAMPLING_RATE
        set(value) {
            if (field == value) return
            field = value
            sampleTime = 1 / value
            sampleCount = ceil(simulationTime * value).toInt()
        }

        /**
         * Период дискретизации измерения сигналов (в условных единицах времени)
         */
        private const val DEFAULT_SAMPLE_TIME = 1 / DEFAULT_SAMPLING_RATE
        private var sampleTime = DEFAULT_SAMPLE_TIME

        /**
         * Количество измерений за время симуляции.
         * Количество отсчетов рекомендуется брать крытным степени 2 (..., 256, 512, 1024, ...)
         * для возможности осуществления БПФ (быстрого преобразования Фурье).
         * Если требование не выполняется, то необходимо дополнять сигнал нулями до кратного
         * степени 2 размера, чтобы выполнять БПФ.
         */
        private const val DEFAULT_SAMPLE_COUNT = 8192
        private var sampleCount = DEFAULT_SAMPLE_COUNT

        /**
         * Общее время симуляции (в условных единицах времени)
         */
        var simulationTime = sampleCount * sampleTime
        set(value) {
            if (field == value) return
            field = value
            sampleCount = ceil(value * samplingRate).toInt()
        }

        /**
         * Подсчет количества сэмплов (измерений/тактов), которое получится (измерится, пройдет)
         * за время <code>time</code>
         *
         * @return количество сэмплов (измерений/тактов)
         */
        fun samplesFor(time: Double): Int {
            return (time * samplingRate).toInt()
        }

        /**
         * Производится вычисление функции <code>timeFunction</code> на всем временном участке
         * симуляции.
         * Общее время симуляции: {@link #simulationTime}
         * Временные интервалы вычисления: {@link #DEFAULT_SAMPLE_TIME}
         *
         * @param timeFunction функция с аргументом времени, по которой вычисляются значения
         *
         * @return Сортированный по времени словарь типа [время, значение]
         */
        fun simulate(timeFunction: (Double) -> Double): TreeMap<Double, Double> {
            val data = TreeMap<Double, Double>()
            for (i in 0 until sampleCount) {
                val time = i * sampleTime
                data[time] = timeFunction(time)
            }
            return data
        }

        /**
         * Вычисляет {@code counts} временных отсчетов в соответствии с {@code samplingRate}.
         * Отсчеты начинаются с нуля.
         *
         * @param samplingRate частота дискретизации - с такой частотой берутся отсчеты на временной шкале
         * @param counts количество отсчетов
         * @return временные отсчеты в секундах
         */
        fun getTimeline(samplingRate: Double, counts: Int): DoubleArray {
            val period = 1 / samplingRate
            return DoubleArray(counts) {
                it * period
            }
        }
    }
}