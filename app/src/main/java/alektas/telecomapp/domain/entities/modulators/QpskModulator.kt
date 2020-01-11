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
        val dataI = data.filterIndexed { index, _ -> index % 2 == 0 }.toDoubleArray()
        val signalI = DigitalSignal(dataI, bitTime * 2)

        val dataQ = data.filterIndexed { index, _ -> index % 2 != 0 }.toDoubleArray()
        val signalQ = DigitalSignal(dataQ, bitTime * 2)

        return carrier * signalI - carrier.shiftPhaseBy(Math.PI / 2) * signalQ
    }

}