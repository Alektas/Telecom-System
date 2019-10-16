package alektas.telecomapp.domain.entities.signals

interface Signal {
    /**
     * Возвращает словарь со всеми значения сигнала на всем измеренном временном участке.
     * Ключ - время измерения, значение - величина (напряжение/ток/мощность) сигнала в момент времени.
     */
    fun getPoints(): Map<Double, Double>

    /**
     * Возвращает словарь со всеми значения сигнала от <code>from</code> до <code>to</code> на временной шкале.
     * Ключ - время измерения, значение - величина (напряжение/ток/мощность) сигнала в момент времени.
     *
     * @param from от какого момента времени брать значения (включительно), в секундах
     * @param to до какого момента времени брать значения (не включительно), в секундах
     */
    fun getPoints(from: Double, to: Double): Map<Double, Double>

    /**
     * Возвращает все значения сигнала на всем измеренном временном участке.
     * Значение - величина (напряжение/ток/мощность) сигнала в момент времени.
     */
    fun getValues(): DoubleArray

    /**
     * Возвращает значения сигнала от <code>from</code> до <code>to</code> на временной шкале.
     * Значение - величина (напряжение/ток/мощность) сигнала в момент времени.
     *
     * @param from от какого момента времени брать значения (включительно), в секундах
     * @param to до какого момента времени брать значения (не включительно), в секундах
     */
    fun getValues(from: Double, to: Double): DoubleArray

    /**
     * Возвращает значение сигнала во время <code>time</code>.
     *
     * @param time момент времени, в котором берется значение, в секундах
     */
    fun getValueAt(time: Double): Double

    operator fun plus(other: Signal): Signal

    operator fun minus(other: Signal): Signal

    operator fun times(other: Signal): Signal

}