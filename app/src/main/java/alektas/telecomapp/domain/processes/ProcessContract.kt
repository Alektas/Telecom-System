package alektas.telecomapp.domain.processes

const val READ_FILE_KEY = "FILE_READING"
const val READ_FILE_NAME = "Чтение файла"
const val CHARACTERISTICS_KEY = "CHARACTERISTICS_CALCULATION"
const val CHARACTERISTICS_NAME = "Расчёт характеристик"
const val BER_CALC_KEY = "BER_CALCULATION"
const val BER_CALC_NAME = "Расчёт BER"
const val THEORETIC_BER_CALC_KEY = "THEORETIC_BER_CALCULATION"
const val THEORETIC_BER_CALC_NAME = "Расчёт теоретической BER"
const val CAPACITY_CALC_KEY = "CAPACITY_CALCULATION"
const val CAPACITY_CALC_NAME = "Расчёт пропускной способности"
const val DATA_SPEED_CALC_KEY = "DATA_SPEED_CALCULATION"
const val DATA_SPEED_CALC_NAME = "Подсчёт скорости передачи"
const val TRANSMITTING_PROCESS_KEY = "FRAMES_TRANSMITTING"
const val TRANSMITTING_PROCESS_NAME = "Передача фреймов"
const val GENERATE_DATA_KEY = "DATA_GENERATING"
const val GENERATE_DATA_NAME = "Генерация данных"
const val CREATE_SIGNAL_KEY = "SIGNAL_CREATION"
const val CREATE_SIGNAL_NAME = "Генерация сигнала"
const val CREATE_NOISE_KEY = "NOISE_CREATION"
const val CREATE_NOISE_NAME = "Генерация шума"
const val CREATE_INTERFERENCE_KEY = "INTERFERENCE_CREATION"
const val CREATE_INTERFERENCE_NAME = "Генерация помех"
const val CREATE_ETHER_KEY = "ETHER_CREATION"
const val CREATE_ETHER_NAME = "Генерация эфира"
const val DEMODULATE_KEY = "DEMODULATION"
const val DEMODULATE_NAME = "Демодулирование"
const val DECODE_KEY = "DECODING"
const val DECODE_NAME = "Декодирование"
const val DETECT_CHANNELS_KEY = "DETECTING_CHANNELS"
const val DETECT_CHANNELS_NAME = "Определение каналов"
const val FIND_ERRORS_KEY = "FINDING_ERRORS"
const val FIND_ERRORS_NAME = "Поиск ошибок"

class ProcessContract