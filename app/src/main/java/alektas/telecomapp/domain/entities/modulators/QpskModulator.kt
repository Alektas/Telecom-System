package alektas.telecomapp.domain.entities.modulators

import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.HarmonicSignal
import alektas.telecomapp.domain.entities.signals.Signal

class QpskModulator(val bitTime: Double) : Modulator<DoubleArray> {

    /**
     * @param carrier несущая гармоника. Для соответствия общепринятой конвенциии необходимо
     * использовать сигнал вида косинус
     * @param data массив данных группового сигнала
     */
    override fun modulate(carrier: HarmonicSignal, data: DoubleArray): Signal {
        val channelSize = (0.5 * data.size).toInt()
        val dataI = DoubleArray(channelSize) { data[it * 2] }
        val dataQ = DoubleArray(channelSize) { data[it * 2 + 1] }

        val symbolTime = bitTime * 2
        val signalI = DigitalSignal(dataI, symbolTime)
        val signalQ = DigitalSignal(dataQ, symbolTime)

        return carrier * signalI - carrier.shiftPhaseBy(Math.PI / 2) * signalQ
    }

}